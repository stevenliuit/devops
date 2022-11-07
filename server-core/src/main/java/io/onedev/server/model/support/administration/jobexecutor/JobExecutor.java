package io.onedev.server.model.support.administration.jobexecutor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.core.Response;

import org.apache.wicket.protocol.ws.api.IWebSocketConnection;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import io.onedev.commons.loader.ExtensionPoint;
import io.onedev.commons.utils.FileUtils;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.job.JobContext;
import io.onedev.server.terminal.ShellSession;
import io.onedev.server.terminal.TerminalManager;
import io.onedev.server.util.ExceptionUtils;
import io.onedev.server.util.PKCS12CertExtractor;
import io.onedev.server.util.ServerConfig;
import io.onedev.server.util.usage.Usage;
import io.onedev.server.util.validation.annotation.DnsName;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.JobAuthorization;
import io.onedev.server.web.editable.annotation.ShowCondition;

@ExtensionPoint
@Editable
public abstract class JobExecutor implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean enabled = true;
	
	private String name;
	
	private String jobAuthorization;
	
	private boolean shellAccessEnabled;
	
	private int cacheTTL = 7;
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Editable(order=10)
	@DnsName //this name may be used as namespace/network prefixes, so put a strict constraint
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=20, description="启用此选项以允许项目经理打开 Web 终端以运行构建。<b class='text-danger'>警告</b>：具有 shell 访问权限的用户可以控制执行程序使用的节点。 您应该在下面配置作业授权，以确保如果启用此选项，执行程序只能由受信任的作业使用")
	@ShowCondition("isTerminalSupported")
	public boolean isShellAccessEnabled() {
		return shellAccessEnabled;
	}

	public void setShellAccessEnabled(boolean shellAccessEnabled) {
		this.shellAccessEnabled = shellAccessEnabled;
	}
	
	@SuppressWarnings("unused")
	private static boolean isTerminalSupported() {
		return OneDev.getInstance(TerminalManager.class).isTerminalSupported();
	}

	@Editable(order=10000, placeholder="可供任何jobs使用", 
			description="可选择指定授权使用此执行程序的作业")
	@JobAuthorization
	@Nullable
	public String getJobAuthorization() {
		return jobAuthorization;
	}

	public void setJobAuthorization(String jobAuthorization) {
		this.jobAuthorization = jobAuthorization;
	}

	@Editable(order=50000, group="更多设置", description="按天指定作业缓存 TTL（生存时间）。系统甚至可以为同一个缓存键创建多个作业缓存，以避免在同时运行作业时发生缓存冲突。 此设置告诉系统删除指定时间段内不活动的缓存以节省磁盘空间")
	public int getCacheTTL() {
		return cacheTTL;
	}

	public void setCacheTTL(int cacheTTL) {
		this.cacheTTL = cacheTTL;
	}
	
	public abstract void execute(JobContext jobContext);
	
	public abstract void resume(JobContext jobContext);
	
	public abstract ShellSession openShell(IWebSocketConnection connection, JobContext jobContext);
	
	public boolean isPlaceholderAllowed() {
		return true;
	}
	
	public Usage onDeleteProject(String projectPath) {
		Usage usage = new Usage();
		if (jobAuthorization != null 
				&& io.onedev.server.job.authorization.JobAuthorization.parse(jobAuthorization).isUsingProject(projectPath)) {
			usage.add("job requirement" );
		}
		return usage;
	}
	
	public void onMoveProject(String oldPath, String newPath) {
		if (jobAuthorization != null) {
			io.onedev.server.job.authorization.JobAuthorization parsedJobAuthorization = 
					io.onedev.server.job.authorization.JobAuthorization.parse(jobAuthorization);
			parsedJobAuthorization.onMoveProject(oldPath, newPath);
			jobAuthorization = parsedJobAuthorization.toString();
		}
	}

	public Usage onDeleteUser(String userName) {
		Usage usage = new Usage();
		if (jobAuthorization != null 
				&& io.onedev.server.job.authorization.JobAuthorization.parse(jobAuthorization).isUsingUser(userName)) {
			usage.add("job authorization" );
		}
		return usage;
	}
	
	public void onRenameUser(String oldName, String newName) {
		if (jobAuthorization != null) {
			io.onedev.server.job.authorization.JobAuthorization parsedJobAuthorization = 
					io.onedev.server.job.authorization.JobAuthorization.parse(jobAuthorization);
			parsedJobAuthorization.onRenameUser(oldName, newName);
			jobAuthorization = parsedJobAuthorization.toString();
		}
	}
	
	protected List<String> getTrustCertContent() {
		List<String> trustCertContent = new ArrayList<>();
		ServerConfig serverConfig = OneDev.getInstance(ServerConfig.class); 
		File keystoreFile = serverConfig.getKeystoreFile();
		if (keystoreFile != null) {
			String password = serverConfig.getKeystorePassword();
			for (Map.Entry<String, String> entry: new PKCS12CertExtractor(keystoreFile, password).extact().entrySet()) 
				trustCertContent.addAll(Splitter.on('\n').trimResults().splitToList(entry.getValue()));
		}
		if (serverConfig.getTrustCertsDir() != null) {
			for (File file: serverConfig.getTrustCertsDir().listFiles()) {
				if (file.isFile()) {
					try {
						trustCertContent.addAll(FileUtils.readLines(file, UTF_8));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return trustCertContent;
	}
	
	protected String getErrorMessage(Exception exception) {
		Response response = ExceptionUtils.buildResponse(exception);
		if (response != null) 
			return response.getEntity().toString();
		else
			return Throwables.getStackTraceAsString(exception);
	}
	
}
