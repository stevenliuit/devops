package io.onedev.server.plugin.executor.serverdocker;

import static io.onedev.agent.DockerExecutorUtils.createNetwork;
import static io.onedev.agent.DockerExecutorUtils.deleteDir;
import static io.onedev.agent.DockerExecutorUtils.deleteNetwork;
import static io.onedev.agent.DockerExecutorUtils.isUseProcessIsolation;
import static io.onedev.agent.DockerExecutorUtils.newDockerKiller;
import static io.onedev.agent.DockerExecutorUtils.startService;
import static io.onedev.k8shelper.KubernetesHelper.cloneRepository;
import static io.onedev.k8shelper.KubernetesHelper.installGitCert;
import static io.onedev.k8shelper.KubernetesHelper.stringifyStepPosition;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.lang3.SystemUtils;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import io.onedev.agent.DockerExecutorUtils;
import io.onedev.agent.ExecutorUtils;
import io.onedev.agent.job.FailedException;
import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.loader.AppLoader;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.PathUtils;
import io.onedev.commons.utils.StringUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.commons.utils.command.Commandline;
import io.onedev.commons.utils.command.ExecutionResult;
import io.onedev.commons.utils.command.LineConsumer;
import io.onedev.k8shelper.BuildImageFacade;
import io.onedev.k8shelper.CacheAllocationRequest;
import io.onedev.k8shelper.CacheInstance;
import io.onedev.k8shelper.CheckoutFacade;
import io.onedev.k8shelper.CloneInfo;
import io.onedev.k8shelper.CommandFacade;
import io.onedev.k8shelper.CompositeFacade;
import io.onedev.k8shelper.JobCache;
import io.onedev.k8shelper.KubernetesHelper;
import io.onedev.k8shelper.LeafFacade;
import io.onedev.k8shelper.LeafHandler;
import io.onedev.k8shelper.OsContainer;
import io.onedev.k8shelper.OsExecution;
import io.onedev.k8shelper.OsInfo;
import io.onedev.k8shelper.RunContainerFacade;
import io.onedev.k8shelper.ServerSideFacade;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.Service;
import io.onedev.server.buildspec.job.JobContext;
import io.onedev.server.buildspec.job.JobManager;
import io.onedev.server.git.config.GitConfig;
import io.onedev.server.job.resource.ResourceManager;
import io.onedev.server.model.support.RegistryLogin;
import io.onedev.server.model.support.administration.jobexecutor.JobExecutor;
import io.onedev.server.plugin.executor.serverdocker.ServerDockerExecutor.TestData;
import io.onedev.server.terminal.CommandlineSession;
import io.onedev.server.terminal.ShellSession;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Horizontal;
import io.onedev.server.web.editable.annotation.OmitName;
import io.onedev.server.web.util.Testable;

@Editable(order=ServerDockerExecutor.ORDER, name="Server Docker Executor", 
		description="This executor runs build jobs as docker containers on OneDev server")
@ClassValidating
@Horizontal
public class ServerDockerExecutor extends JobExecutor implements Testable<TestData>, Validatable {

	private static final long serialVersionUID = 1L;
	
	static final int ORDER=50;

	private static final Object cacheHomeCreationLock = new Object();
	
	private List<RegistryLogin> registryLogins = new ArrayList<>();
	
	private String runOptions;
	
	private String dockerExecutable;
	
	private boolean mountDockerSock;
	
	private transient volatile File hostBuildHome;
	
	private transient volatile LeafFacade runningStep;
	
	private transient volatile String containerName;
	
	private static transient volatile String hostInstallPath;
	
	@Editable(order=400, description="如有必要，指定 docker 注册表的登录信息")
	public List<RegistryLogin> getRegistryLogins() {
		return registryLogins;
	}

	public void setRegistryLogins(List<RegistryLogin> registryLogins) {
		this.registryLogins = registryLogins;
	}

	@Editable(order=500, description="是否将 docker sock 挂载到作业容器中以支持作业命令中的 docker 操作，例如构建 docker 映像。<br><b class='text-danger'>WARNING</b>：恶意作业可以控制 通过操作安装的 docker sock 整个系统。 您应该在下面配置作业授权，以确保如果启用此选项，执行程序只能由受信任的作业使用")
	public boolean isMountDockerSock() {
		return mountDockerSock;
	}

	public void setMountDockerSock(boolean mountDockerSock) {
		this.mountDockerSock = mountDockerSock;
	}

	@Editable(order=50050, group="更多设置", description="可以选择指定运行容器的选项。 例如，您可以使用 <tt>-m 2g</tt> 将创建的容器的内存限制为 2 GB")
	public String getRunOptions() {
		return runOptions;
	}

	public void setRunOptions(String runOptions) {
		this.runOptions = runOptions;
	}

	@Editable(order=50100, group="更多设置", placeholder="默认情况下使用", description=""
			+ "可选择指定 docker 可执行文件，例如 <i>/usr/local/bin/docker</i>。留空以在 PATH 中使用 docker 可执行文件")
	public String getDockerExecutable() {
		return dockerExecutable;
	}

	public void setDockerExecutable(String dockerExecutable) {
		this.dockerExecutable = dockerExecutable;
	}

	private Commandline newDocker() {
		if (getDockerExecutable() != null)
			return new Commandline(getDockerExecutable());
		else if (SystemUtils.IS_OS_MAC_OSX && new File("/usr/local/bin/docker").exists())
			return new Commandline("/usr/local/bin/docker");
		else
			return new Commandline("docker");
	}
	
	private File getCacheHome(JobExecutor jobExecutor) {
		File file = new File(Bootstrap.getSiteDir(), "cache/" + jobExecutor.getName());
		if (!file.exists()) synchronized (cacheHomeCreationLock) {
			FileUtils.createDir(file);
		}
		return file;
	}
	
	@Override
	public void execute(JobContext jobContext) {
		if (OneDev.getK8sService() != null) {
			throw new ExplicitException(""
					+ "OneDev running inside kubernetes cluster does not support server docker executor. "
					+ "Please use kubernetes executor instead");
		}
		
		hostBuildHome = FileUtils.createTempDir("onedev-build");
		try {
			TaskLogger jobLogger = jobContext.getLogger();
			OneDev.getInstance(ResourceManager.class).run(new Runnable() {

				@Override
				public void run() {
					String network = getName() + "-" + jobContext.getProjectId() + "-" 
							+ jobContext.getBuildNumber() + "-" + jobContext.getRetried();

					jobLogger.log(String.format("Executing job (executor: %s, network: %s)...", getName(), network));
					jobContext.notifyJobRunning(null);
					
					JobManager jobManager = OneDev.getInstance(JobManager.class);		
					File hostCacheHome = getCacheHome(jobContext.getJobExecutor());
					
					jobLogger.log("Setting up job cache...") ;
					JobCache cache = new JobCache(hostCacheHome) {

						@Override
						protected Map<CacheInstance, String> allocate(CacheAllocationRequest request) {
							return jobManager.allocateJobCaches(jobContext.getJobToken(), request);
						}

						@Override
						protected void delete(File cacheDir) {
							deleteDir(cacheDir, newDocker(), Bootstrap.isInDocker());							
						}
						
					};
					cache.init(false);

					login(jobLogger);
					
					createNetwork(newDocker(), network, jobLogger);
					try {
						OsInfo osInfo = OneDev.getInstance(OsInfo.class);
						
						for (Service jobService: jobContext.getServices()) {
							jobLogger.log("Starting service (name: " + jobService.getName() + ", image: " + jobService.getImage() + ")...");
							startService(newDocker(), network, jobService.toMap(), osInfo, jobLogger);
						}
						
						File hostWorkspace = new File(hostBuildHome, "workspace");
						FileUtils.createDir(hostWorkspace);
						
						AtomicReference<File> hostAuthInfoHome = new AtomicReference<>(null);
						try {						
							cache.installSymbolinks(hostWorkspace);
							
							jobLogger.log("Copying job dependencies...");
							jobContext.copyDependencies(hostWorkspace);
							
							String containerBuildHome;
							String containerWorkspace;
							if (SystemUtils.IS_OS_WINDOWS) {
								containerBuildHome = "C:\\onedev-build";
								containerWorkspace = "C:\\onedev-build\\workspace";
							} else {
								containerBuildHome = "/onedev-build";
								containerWorkspace = "/onedev-build/workspace";
							}
							
							jobContext.reportJobWorkspace(containerWorkspace);
							CompositeFacade entryFacade = new CompositeFacade(jobContext.getActions());
							boolean successful = entryFacade.execute(new LeafHandler() {

								private int runStepContainer(String image, @Nullable String entrypoint, 
										List<String> arguments, Map<String, String> environments, 
										@Nullable String workingDir, Map<String, String> volumeMounts, 
										List<Integer> position, boolean useTTY) {
									// Uninstall symbol links as docker can not process it well
									cache.uninstallSymbolinks(hostWorkspace);
									containerName = network + "-step-" + stringifyStepPosition(position);
									try {
										Commandline docker = newDocker();
										docker.addArgs("run", "--name=" + containerName, "--network=" + network);
										if (getRunOptions() != null)
											docker.addArgs(StringUtils.parseQuoteTokens(getRunOptions()));
										
										docker.addArgs("-v", getHostPath(hostBuildHome.getAbsolutePath()) + ":" + containerBuildHome);
										
										for (Map.Entry<String, String> entry: volumeMounts.entrySet()) {
											if (entry.getKey().contains(".."))
												throw new ExplicitException("Volume mount source path should not contain '..'");
											String hostPath = getHostPath(new File(hostWorkspace, entry.getKey()).getAbsolutePath());
											docker.addArgs("-v", hostPath + ":" + entry.getValue());
										}
										
										if (entrypoint != null) {
											docker.addArgs("-w", containerWorkspace);
										} else if (workingDir != null) { 
											if (workingDir.contains(".."))
												throw new ExplicitException("Container working dir should not contain '..'");
											docker.addArgs("-w", workingDir);
										}
										
										for (Map.Entry<CacheInstance, String> entry: cache.getAllocations().entrySet()) {
											String hostCachePath = entry.getKey().getDirectory(hostCacheHome).getAbsolutePath();
											String containerCachePath = PathUtils.resolve(containerWorkspace, entry.getValue());
											docker.addArgs("-v", getHostPath(hostCachePath) + ":" + containerCachePath);
										}
										
										if (isMountDockerSock()) {
											if (SystemUtils.IS_OS_WINDOWS) 
												docker.addArgs("-v", "//./pipe/docker_engine://./pipe/docker_engine");
											else
												docker.addArgs("-v", "/var/run/docker.sock:/var/run/docker.sock");
										}
										
										if (hostAuthInfoHome.get() != null) {
											String hostPath = getHostPath(hostAuthInfoHome.get().getAbsolutePath());
											if (SystemUtils.IS_OS_WINDOWS) {
												docker.addArgs("-v",  hostPath + ":C:\\Users\\ContainerAdministrator\\auth-info");
												docker.addArgs("-v",  hostPath + ":C:\\Users\\ContainerUser\\auth-info");
											} else { 
												docker.addArgs("-v", hostPath + ":/root/auth-info");
											}
										}
										
										for (Map.Entry<String, String> entry: environments.entrySet()) 
											docker.addArgs("-e", entry.getKey() + "=" + entry.getValue());
										
										docker.addArgs("-e", "ONEDEV_WORKSPACE=" + containerWorkspace);
	
										if (useTTY)
											docker.addArgs("-t");
										
										if (entrypoint != null)
											docker.addArgs("--entrypoint=" + entrypoint);
										
										if (isUseProcessIsolation(newDocker(), image, osInfo, jobLogger))
											docker.addArgs("--isolation=process");
										
										docker.addArgs(image);
										docker.addArgs(arguments.toArray(new String[arguments.size()]));
										
										ExecutionResult result = docker.execute(ExecutorUtils.newInfoLogger(jobLogger), 
												ExecutorUtils.newWarningLogger(jobLogger), null, newDockerKiller(newDocker(), 
												containerName, jobLogger));
										return result.getReturnCode();
									} finally {
										containerName = null;
										cache.installSymbolinks(hostWorkspace);
									}
								}
								
								@Override
								public boolean execute(LeafFacade facade, List<Integer> position) {
									runningStep = facade;
									try {
										String stepNames = entryFacade.getNamesAsString(position);
										jobLogger.notice("Running step \"" + stepNames + "\"...");
										
										if (facade instanceof CommandFacade) {
											CommandFacade commandFacade = (CommandFacade) facade;
	
											OsExecution execution = commandFacade.getExecution(osInfo);
											if (execution.getImage() == null) {
												throw new ExplicitException("This step can only be executed by server shell "
														+ "executor or remote shell executor");
											}
											
											Commandline entrypoint = DockerExecutorUtils.getEntrypoint(
													hostBuildHome, commandFacade, osInfo, hostAuthInfoHome.get() != null);
											
											int exitCode = runStepContainer(execution.getImage(), entrypoint.executable(), 
													entrypoint.arguments(), new HashMap<>(), null, new HashMap<>(), 
													position, commandFacade.isUseTTY());
											
											if (exitCode != 0) {
												jobLogger.error("Step \"" + stepNames + "\" is failed: Command exited with code " + exitCode);
												return false;
											}
										} else if (facade instanceof BuildImageFacade || facade instanceof BuildImageFacade) {
											DockerExecutorUtils.buildImage(newDocker(), (BuildImageFacade) facade, 
													hostBuildHome, jobLogger);
										} else if (facade instanceof RunContainerFacade) {
											RunContainerFacade rubContainerFacade = (RunContainerFacade) facade;
											OsContainer container = rubContainerFacade.getContainer(osInfo);
											List<String> arguments = new ArrayList<>();
											if (container.getArgs() != null)
												arguments.addAll(Arrays.asList(StringUtils.parseQuoteTokens(container.getArgs())));
											int exitCode = runStepContainer(container.getImage(), null, arguments, container.getEnvMap(), 
													container.getWorkingDir(), container.getVolumeMounts(), position, rubContainerFacade.isUseTTY());
											if (exitCode != 0) {
												jobLogger.error("Step \"" + stepNames + "\" is failed: Container exited with code " + exitCode);
												return false;
											} 
										} else if (facade instanceof CheckoutFacade) {
											try {
												CheckoutFacade checkoutFacade = (CheckoutFacade) facade;
												jobLogger.log("Checking out code...");
												if (hostAuthInfoHome.get() == null)
													hostAuthInfoHome.set(FileUtils.createTempDir());
												Commandline git = new Commandline(AppLoader.getInstance(GitConfig.class).getExecutable());	
												
												checkoutFacade.setupWorkingDir(git, hostWorkspace);
												git.environments().put("HOME", hostAuthInfoHome.get().getAbsolutePath());
		
												CloneInfo cloneInfo = checkoutFacade.getCloneInfo();
												
												cloneInfo.writeAuthData(hostAuthInfoHome.get(), git, ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
												try {
													List<String> trustCertContent = getTrustCertContent();
													if (!trustCertContent.isEmpty()) {
														installGitCert(new File(hostAuthInfoHome.get(), "trust-cert.pem"), trustCertContent, 
																git, ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
													}
			
													int cloneDepth = checkoutFacade.getCloneDepth();
													
													cloneRepository(git, jobContext.getProjectGitDir().getAbsolutePath(), 
															cloneInfo.getCloneUrl(), jobContext.getRefName(), jobContext.getCommitId().name(), 
															checkoutFacade.isWithLfs(), checkoutFacade.isWithSubmodules(),
															cloneDepth, ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
												} finally {
													git.clearArgs();
													git.addArgs("config", "--global", "--unset", "core.sshCommand");
													ExecutionResult result = git.execute(ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
													if (result.getReturnCode() != 5 && result.getReturnCode() != 0)
														result.checkReturnCode();
												}
											} catch (Exception e) {
												jobLogger.error("Step \"" + stepNames + "\" is failed: " + getErrorMessage(e));
												return false;
											}
										} else {
											ServerSideFacade serverSideFacade = (ServerSideFacade) facade;
											try {
												serverSideFacade.execute(hostBuildHome, new ServerSideFacade.Runner() {
													
													@Override
													public Map<String, byte[]> run(File inputDir, Map<String, String> placeholderValues) {
														return jobContext.runServerStep(position, inputDir, placeholderValues, jobLogger);
													}
													
												});
											} catch (Exception e) {
												jobLogger.error("Step \"" + stepNames + "\" is failed: " + getErrorMessage(e));
												return false;
											}
										}
										jobLogger.success("Step \"" + stepNames + "\" is successful");
										return true;
									} finally {
										runningStep = null;
									}
								}

								@Override
								public void skip(LeafFacade facade, List<Integer> position) {
									jobLogger.notice("Step \"" + entryFacade.getNamesAsString(position) + "\" is skipped");
								}
								
							}, new ArrayList<>());

							if (!successful)
								throw new FailedException();
						} finally {
							cache.uninstallSymbolinks(hostWorkspace);
							// Fix https://code.onedev.io/projects/160/issues/597
							if (SystemUtils.IS_OS_WINDOWS)
								FileUtils.deleteDir(hostWorkspace);
							if (hostAuthInfoHome.get() != null)
								FileUtils.deleteDir(hostAuthInfoHome.get());
						}
					} finally {
						deleteNetwork(newDocker(), network, jobLogger);
					}					
				}
				
			}, jobContext.getResourceRequirements(), jobLogger);
		} finally {
			synchronized (hostBuildHome) {
				deleteDir(hostBuildHome, newDocker(), Bootstrap.isInDocker());
			}
		}
	}

	@Override
	public void resume(JobContext jobContext) {
		if (hostBuildHome != null) synchronized (hostBuildHome) {
			if (hostBuildHome.exists()) 
				FileUtils.touchFile(new File(hostBuildHome, "continue"));
		}
	}

	private void login(TaskLogger jobLogger) {
		for (RegistryLogin login: getRegistryLogins()) 
			DockerExecutorUtils.login(newDocker(), login.getRegistryUrl(), login.getUserName(), login.getPassword(), jobLogger);
	}
	
	private boolean hasOptions(String[] arguments, String... options) {
		for (String argument: arguments) {
			for (String option: options) {
				if (option.startsWith("--")) {
					if (argument.startsWith(option + "=") || argument.equals(option))
						return true;
				} else if (option.startsWith("-")) {
					if (argument.startsWith(option))
						return true;
				} else {
					throw new ExplicitException("Invalid option: " + option);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean isValid = true;
		Set<String> registryUrls = new HashSet<>();
		for (RegistryLogin login: getRegistryLogins()) {
			if (!registryUrls.add(login.getRegistryUrl())) {
				isValid = false;
				String message;
				if (login.getRegistryUrl() != null)
					message = "Duplicate login entry for registry '" + login.getRegistryUrl() + "'";
				else
					message = "Duplicate login entry for official registry";
				context.buildConstraintViolationWithTemplate(message)
						.addPropertyNode("registryLogins").addConstraintViolation();
				break;
			}
		}
		if (getRunOptions() != null) {
			String[] arguments = StringUtils.parseQuoteTokens(getRunOptions());
			String reservedOptions[] = new String[] {"-w", "--workdir", "-d", "--detach", "-a", "--attach", "-t", "--tty", 
					"-i", "--interactive", "--rm", "--restart", "--name"}; 
			if (hasOptions(arguments, reservedOptions)) {
				StringBuilder errorMessage = new StringBuilder("Can not use options: "
						+ Joiner.on(", ").join(reservedOptions));
				context.buildConstraintViolationWithTemplate(errorMessage.toString())
						.addPropertyNode("runOptions").addConstraintViolation();
				isValid = false;
			} 
		}
		if (!isValid)
			context.disableDefaultConstraintViolation();
		return isValid;
	}
	
	private String getHostPath(String path) {
		String installPath = Bootstrap.installDir.getAbsolutePath();
		Preconditions.checkState(path.startsWith(installPath + "/")
				|| path.startsWith(installPath + "\\"));
		if (hostInstallPath == null) {
			if (Bootstrap.isInDocker()) 
				hostInstallPath = DockerExecutorUtils.getHostPath(newDocker(), installPath);
			else 
				hostInstallPath = installPath;
		}
		return hostInstallPath + path.substring(installPath.length());
	}
	
	@Override
	public void test(TestData testData, TaskLogger jobLogger) {
		OneDev.getInstance(ResourceManager.class).run(new Runnable() {

			@Override
			public void run() {
				login(jobLogger);
				
				File workspaceDir = null;
				File cacheDir = null;

				Commandline docker = newDocker();
				try {
					workspaceDir = FileUtils.createTempDir("workspace");
					cacheDir = new File(getCacheHome(ServerDockerExecutor.this), UUID.randomUUID().toString());
					FileUtils.createDir(cacheDir);
					
					jobLogger.log("Testing specified docker image...");
					docker.clearArgs();
					docker.addArgs("run", "--rm");
					if (getRunOptions() != null)
						docker.addArgs(StringUtils.parseQuoteTokens(getRunOptions()));
					String containerWorkspacePath;
					String containerCachePath;
					if (SystemUtils.IS_OS_WINDOWS) {
						containerWorkspacePath = "C:\\onedev-build\\workspace";
						containerCachePath = "C:\\onedev-build\\cache";
					} else {
						containerWorkspacePath = "/onedev-build/workspace";
						containerCachePath = "/onedev-build/cache";
					}
					docker.addArgs("-v", getHostPath(workspaceDir.getAbsolutePath()) + ":" + containerWorkspacePath);
					docker.addArgs("-v", getHostPath(cacheDir.getAbsolutePath()) + ":" + containerCachePath);
					
					docker.addArgs("-w", containerWorkspacePath);
					docker.addArgs(testData.getDockerImage());
					
					if (SystemUtils.IS_OS_WINDOWS) 
						docker.addArgs("cmd", "/c", "echo hello from container");
					else 
						docker.addArgs("sh", "-c", "echo hello from container");
					
					docker.execute(new LineConsumer() {

						@Override
						public void consume(String line) {
							jobLogger.log(line);
						}
						
					}, new LineConsumer() {

						@Override
						public void consume(String line) {
							jobLogger.log(line);
						}
						
					}).checkReturnCode();
				} finally {
					if (workspaceDir != null)
						FileUtils.deleteDir(workspaceDir);
					if (cacheDir != null)
						FileUtils.deleteDir(cacheDir);
				}
				
				if (!SystemUtils.IS_OS_WINDOWS) {
					jobLogger.log("Checking busybox availability...");
					docker = newDocker();
					docker.addArgs("run", "--rm", "busybox", "sh", "-c", "echo hello from busybox");			
					docker.execute(new LineConsumer() {

						@Override
						public void consume(String line) {
							jobLogger.log(line);
						}
						
					}, new LineConsumer() {

						@Override
						public void consume(String line) {
							jobLogger.log(line);
						}
						
					}).checkReturnCode();
				}
				
				Commandline git = new Commandline(AppLoader.getInstance(GitConfig.class).getExecutable());
				KubernetesHelper.testGitLfsAvailability(git, jobLogger);
			}
			
		}, new HashMap<>(), jobLogger);
		
	}
	
	@Editable(name="Specify a Docker Image to Test Against")
	public static class TestData implements Serializable {

		private static final long serialVersionUID = 1L;

		private String dockerImage;

		@Editable
		@OmitName
		@NotEmpty
		public String getDockerImage() {
			return dockerImage;
		}

		public void setDockerImage(String dockerImage) {
			this.dockerImage = dockerImage;
		}
		
	}

	@Override
	public ShellSession openShell(IWebSocketConnection connection, JobContext jobContext) {
		String containerNameCopy = containerName;
		if (containerNameCopy != null) {
			Commandline docker = newDocker();
			docker.addArgs("exec", "-it", containerNameCopy);
			if (runningStep instanceof CommandFacade) {
				CommandFacade commandStep = (CommandFacade) runningStep;
				docker.addArgs(commandStep.getShell(SystemUtils.IS_OS_WINDOWS, null));
			} else if (SystemUtils.IS_OS_WINDOWS) {
				docker.addArgs("cmd");
			} else {
				docker.addArgs("sh");
			}
			return new CommandlineSession(connection, docker);
		} else if (hostBuildHome != null) {
			Commandline shell;
			if (SystemUtils.IS_OS_WINDOWS)
				shell = new Commandline("cmd");
			else
				shell = new Commandline("sh");
			shell.workingDir(new File(hostBuildHome, "workspace"));
			return new CommandlineSession(connection, shell);
		} else {
			throw new ExplicitException("Shell not ready");
		}
	}

}