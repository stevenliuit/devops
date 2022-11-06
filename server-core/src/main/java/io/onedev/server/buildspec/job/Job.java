package io.onedev.server.buildspec.job;

import static io.onedev.server.model.Build.NAME_BRANCH;
import static io.onedev.server.model.Build.NAME_COMMIT;
import static io.onedev.server.model.Build.NAME_JOB;
import static io.onedev.server.model.Build.NAME_PULL_REQUEST;
import static io.onedev.server.model.Build.NAME_TAG;
import static io.onedev.server.search.entity.build.BuildQuery.getRuleName;
import static io.onedev.server.search.entity.build.BuildQueryLexer.And;
import static io.onedev.server.search.entity.build.BuildQueryLexer.InPipelineOf;
import static io.onedev.server.search.entity.build.BuildQueryLexer.Is;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.core.HttpHeaders;

import org.apache.wicket.Component;
import org.eclipse.jgit.lib.ObjectId;
import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputCompletion;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.k8shelper.CloneInfo;
import io.onedev.k8shelper.KubernetesHelper;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.BuildSpecAware;
import io.onedev.server.buildspec.NamedElement;
import io.onedev.server.buildspec.job.action.PostBuildAction;
import io.onedev.server.buildspec.job.projectdependency.ProjectDependency;
import io.onedev.server.buildspec.job.trigger.JobTrigger;
import io.onedev.server.buildspec.param.ParamUtils;
import io.onedev.server.buildspec.param.spec.ParamSpec;
import io.onedev.server.buildspec.step.Step;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.git.GitUtils;
import io.onedev.server.job.authorization.JobAuthorization;
import io.onedev.server.model.Build;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.support.administration.jobexecutor.JobExecutor;
import io.onedev.server.util.ComponentContext;
import io.onedev.server.util.ProjectAndBranch;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.RetryCondition;
import io.onedev.server.web.editable.annotation.SuggestionProvider;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.util.SuggestionUtils;
import io.onedev.server.web.util.WicketUtils;

@Editable
@ClassValidating
public class Job implements NamedElement, Serializable, Validatable {

	private static final long serialVersionUID = 1L;
	
	public static final String SELECTION_PREFIX = "jobs/";
	
	public static final String PROP_NAME = "name";
	
	public static final String PROP_JOB_DEPENDENCIES = "jobDependencies";
	
	public static final String PROP_REQUIRED_SERVICES = "requiredServices";
	
	public static final String PROP_TRIGGERS = "triggers";
	
	public static final String PROP_STEPS = "steps";
	
	public static final String PROP_RETRY_CONDITION = "retryCondition";
	
	public static final String PROP_POST_BUILD_ACTIONS = "postBuildActions";
	
	private String name;
	
	private String jobExecutor;
	
	private List<Step> steps = new ArrayList<>();
	
	private List<ParamSpec> paramSpecs = new ArrayList<>();
	
	private List<JobDependency> jobDependencies = new ArrayList<>();
	
	private List<ProjectDependency> projectDependencies = new ArrayList<>();
	
	private List<String> requiredServices = new ArrayList<>();
	
	private List<JobTrigger> triggers = new ArrayList<>();
	
	private List<CacheSpec> caches = new ArrayList<>();

	private int cpuRequirement = 250;
	
	private int memoryRequirement = 256;
	
	private long timeout = 3600;
	
	private List<PostBuildAction> postBuildActions = new ArrayList<>();
	
	private String retryCondition = "never";
	
	private int maxRetries = 3;
	
	private int retryDelay = 30;
	
	private transient Map<String, ParamSpec> paramSpecMap;
	
	@Editable(order=100, description="指定作业名称")
	@SuggestionProvider("getNameSuggestions")
	@NotEmpty
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@SuppressWarnings("unused")
	private static List<InputCompletion> getNameSuggestions(InputStatus status) {
		BuildSpec buildSpec = BuildSpec.get();
		if (buildSpec != null) {
			List<String> candidates = new ArrayList<>(buildSpec.getJobMap().keySet());
			buildSpec.getJobs().forEach(it->candidates.remove(it.getName()));
			return BuildSpec.suggestOverrides(candidates, status);
		}
		return new ArrayList<>();
	}

	@Editable(order=200, placeholder="使用任何适用的Executor", description="可选择指定执行者 "
			+ "执行此作业。 留空以使用任何执行器，只要满足其工作要求")
	@Interpolative(literalSuggester="suggestJobExecutors", variableSuggester="suggestVariables")
	public String getJobExecutor() {
		return jobExecutor;
	}

	public void setJobExecutor(String jobExecutor) {
		this.jobExecutor = jobExecutor;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestJobExecutors(String matchWith) {
		List<String> applicableJobExecutors = new ArrayList<>();
		ProjectBlobPage page = (ProjectBlobPage) WicketUtils.getPage();
		ProjectAndBranch projectAndBranch = new ProjectAndBranch(page.getProject(), page.getBlobIdent().revision);
		for (JobExecutor executor: OneDev.getInstance(SettingManager.class).getJobExecutors()) {
			if (executor.isEnabled()) {
				if (executor.getJobAuthorization() == null) {
					applicableJobExecutors.add(executor.getName());
				} else {
					if (JobAuthorization.parse(executor.getJobAuthorization()).matches(projectAndBranch))
						applicableJobExecutors.add(executor.getName());
				}
			}
		}
		
		return SuggestionUtils.suggest(applicableJobExecutors, matchWith);
	}
	
	@Editable(order=200, description="步骤将在同一个节点上串行执行，共享同一个 <a href='$docRoot/pages/concepts.md#job-workspace'>job workspace</a>")
	public List<Step> getSteps() {
		return steps;
	}
	
	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	@Editable(order=400, name="参数规格", group="参数和触发器", description="可选择定义作业的参数规范")
	@Valid
	public List<ParamSpec> getParamSpecs() {
		return paramSpecs;
	}

	public void setParamSpecs(List<ParamSpec> paramSpecs) {
		this.paramSpecs = paramSpecs;
	}

	@Editable(order=500, group="参数和触发器", description="使用触发器在特定条件下自动运行作业")
	@Valid
	public List<JobTrigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<JobTrigger> triggers) {
		this.triggers = triggers;
	}

	@Editable(name="工作依赖", order=9110, group="依赖项和服务", description="作业依赖决定了顺序和 "
			+ "运行不同作业时的并发性。 您还可以指定要从上游作业中检索的工件")
	@Valid
	public List<JobDependency> getJobDependencies() {
		return jobDependencies;
	}

	public void setJobDependencies(List<JobDependency> jobDependencies) {
		this.jobDependencies = jobDependencies;
	}

	@Editable(name="项目依赖", order=9112, group="依赖项和服务", description="使用项目依赖检索 "
			+ "来自其他项目的工件")
	@Valid
	public List<ProjectDependency> getProjectDependencies() {
		return projectDependencies;
	}

	public void setProjectDependencies(List<ProjectDependency> projectDependencies) {
		this.projectDependencies = projectDependencies;
	}

	@Editable(order=9114, group="依赖项和服务", placeholder="无需服务", 
			description="可选择指定此作业所需的服务. "
			+ "<b class='text-warning'>NOTE:</b> 服务仅受 docker 感知执行器支持 "
			+ "(服务器 docker 执行器、远程 docker 执行器或 kubernetes 执行器)")
	@ChoiceProvider("getServiceChoices")
	public List<String> getRequiredServices() {
		return requiredServices;
	}

	public void setRequiredServices(List<String> requiredServices) {
		this.requiredServices = requiredServices;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getServiceChoices() {
		List<String> choices = new ArrayList<>();
		Component component = ComponentContext.get().getComponent();
		BuildSpecAware buildSpecAware = WicketUtils.findInnermost(component, BuildSpecAware.class);
		if (buildSpecAware != null) {
			BuildSpec buildSpec = buildSpecAware.getBuildSpec();
			if (buildSpec != null) { 
				choices.addAll(buildSpec.getServiceMap().values().stream()
						.map(it->it.getName()).collect(Collectors.toList()));
			}
		}
		return choices;
	}

	@Editable(order=9400, group="更多设置", description="指定在失败时重试构建的条件")
	@NotEmpty
	@RetryCondition
	public String getRetryCondition() {
		return retryCondition;
	}

	public void setRetryCondition(String retryCondition) {
		this.retryCondition = retryCondition;
	}

	@Editable(order=9410, group="更多设置", description="放弃前的最大重试次数")
	@Min(value=1, message="此值不应小于 1")
	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	@Editable(order=9420, group="更多设置", description="第一次重试的延迟（以秒为单位）. "
			+ "后续重试的延迟将使用指数计算back-off "
			+ "基于这个延迟")
	@Min(value=1, message="此值不应小于 1")
	public int getRetryDelay() {
		return retryDelay;
	}

	public void setRetryDelay(int retryDelay) {
		this.retryDelay = retryDelay;
	}
	
	@Editable(order=10050, name="CPU 要求", group="更多设置", description="以毫秒为单位指定作业的 CPU 要求. "
			+ "1000 毫秒表示单个 CPU 内核")
	public int getCpuRequirement() {
		return cpuRequirement;
	}

	public void setCpuRequirement(int cpuRequirement) {
		this.cpuRequirement = cpuRequirement;
	}

	@Editable(order=10060, group="更多设置", description="以兆字节指定作业的内存需求")
	public int getMemoryRequirement() {
		return memoryRequirement;
	}

	public void setMemoryRequirement(int memoryRequirement) {
		this.memoryRequirement = memoryRequirement;
	}

	@Editable(order=10100, group="更多设置", description="缓存特定路径以加速作业执行. "
			+ "例如，对于由各种 docker 执行程序执行的 Java Maven 项目，您可以缓存文件夹 "
			+ "<tt>/root/.m2/repository</tt> 避免为后续执行下载依赖项.<br>"
			+ "<b class='text-danger'>WARNING</b>: 使用缓存时，恶意作业与相同的作业执行器一起运行 "
			+ "可以使用与您相同的缓存键故意读取甚至污染缓存。 为了避免这个问题, "
			+ "请确保执行您的作业的作业执行者只能通过作业由受信任的作业使用 "
			+ "授权设置</b>")
	@Valid
	public List<CacheSpec> getCaches() {
		return caches;
	}

	public void setCaches(List<CacheSpec> caches) {
		this.caches = caches;
	}

	@Editable(order=10500, group="更多设置", description="以秒为单位指定超时")
	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	@Editable(order=10600, name="后期构建操作", group="更多设置")
	@Valid
	public List<PostBuildAction> getPostBuildActions() {
		return postBuildActions;
	}
	
	public void setPostBuildActions(List<PostBuildAction> postBuildActions) {
		this.postBuildActions = postBuildActions;
	}
	
	@Nullable
	public JobTriggerMatch getTriggerMatch(ProjectEvent event) {
		for (JobTrigger trigger: getTriggers()) {
			SubmitReason reason = trigger.matches(event, this);
			if (reason != null)
				return new JobTriggerMatch(trigger, reason);
		}
		return null;
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean isValid = true;
		
		Set<String> keys = new HashSet<>();
		Set<String> paths = new HashSet<>();
		for (CacheSpec cache: caches) {
			if (!keys.add(cache.getKey())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("重复键 (" + cache.getKey() + ")")
						.addPropertyNode("caches").addConstraintViolation();
			}
			if (!paths.add(cache.getPath())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("重复路径 (" + cache.getPath() + ")")
						.addPropertyNode("caches").addConstraintViolation();
			} 
		}

		Set<String> dependencyJobNames = new HashSet<>();
		for (JobDependency dependency: jobDependencies) {
			if (!dependencyJobNames.add(dependency.getJobName())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("重复依赖 (" + dependency.getJobName() + ")")
						.addPropertyNode("jobDependencies").addConstraintViolation();
			} 
		}
		
		Set<String> dependencyProjectPaths = new HashSet<>();
		for (ProjectDependency dependency: projectDependencies) {
			if (!dependencyProjectPaths.add(dependency.getProjectPath())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("重复依赖 (" + dependency.getProjectPath() + ")")
						.addPropertyNode("projectDependencies").addConstraintViolation();
			}
		}
		
		Set<String> paramSpecNames = new HashSet<>();
		for (ParamSpec paramSpec: paramSpecs) {
			if (!paramSpecNames.add(paramSpec.getName())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("重复的参数规范 (" + paramSpec.getName() + ")")
						.addPropertyNode("paramSpecs").addConstraintViolation();
			} 
		}
		
		if (getRetryCondition() != null) { 
			try {
				io.onedev.server.buildspec.job.retrycondition.RetryCondition.parse(this, getRetryCondition());
			} catch (Exception e) {
				String message = e.getMessage();
				if (message == null)
					message = "重试条件格式错误";
				context.buildConstraintViolationWithTemplate(message)
						.addPropertyNode(PROP_RETRY_CONDITION)
						.addConstraintViolation();
				isValid = false;
			}
		}
		
		if (isValid) {
			for (int triggerIndex=0; triggerIndex<getTriggers().size(); triggerIndex++) {
				JobTrigger trigger = getTriggers().get(triggerIndex);
				try {
					ParamUtils.validateParams(getParamSpecs(), trigger.getParams());
				} catch (Exception e) {
					String errorMessage = String.format("验证作业参数时出错 (item: #%s, error message: %s)", 
							(triggerIndex+1), e.getMessage());
					context.buildConstraintViolationWithTemplate(errorMessage)
							.addPropertyNode(PROP_TRIGGERS)
							.addConstraintViolation();
					isValid = false;
				}
			}
		}
		
		if (!isValid)
			context.disableDefaultConstraintViolation();
		
		return isValid;
	}
	
	public Map<String, ParamSpec> getParamSpecMap() {
		if (paramSpecMap == null)
			paramSpecMap = ParamUtils.getParamSpecMap(paramSpecs);
		return paramSpecMap;
	}
	
	public static String getBuildQuery(ObjectId commitId, String jobName, 
			@Nullable Build pipelineOf, @Nullable String refName, @Nullable PullRequest request) {
		String query = "" 
				+ Criteria.quote(NAME_COMMIT) + " " + getRuleName(Is) + " " + Criteria.quote(commitId.name()) 
				+ " " + getRuleName(And) + " "
				+ Criteria.quote(NAME_JOB) + " " + getRuleName(Is) + " " + Criteria.quote(jobName);
		if (pipelineOf != null) 
			query = query + " " + getRuleName(And) + " " + getRuleName(InPipelineOf) + " " + Criteria.quote("#" + pipelineOf.getNumber());
		if (request != null) {
			query = query 
					+ " " + getRuleName(And) + " " 
					+ Criteria.quote(NAME_PULL_REQUEST) + " " + getRuleName(Is) + " " + Criteria.quote("#" + request.getNumber());
		}
		if (refName != null) {
			String branch = GitUtils.ref2branch(refName);
			if (branch != null) {
				query = query 
					+ " " + getRuleName(And) + " " 
					+ Criteria.quote(NAME_BRANCH) + " " + getRuleName(Is) + " " + Criteria.quote(branch);
			} 
			String tag = GitUtils.ref2tag(refName);
			if (tag != null) {
				query = query 
					+ " " + getRuleName(And) + " " 
					+ Criteria.quote(NAME_TAG) + " " + getRuleName(Is) + " " + Criteria.quote(tag);
			} 
		}
		return query;
	}
	
	public static List<String> getChoices() {
		List<String> choices = new ArrayList<>();
		Component component = ComponentContext.get().getComponent();
		BuildSpecAware buildSpecAware = WicketUtils.findInnermost(component, BuildSpecAware.class);
		if (buildSpecAware != null) {
			BuildSpec buildSpec = buildSpecAware.getBuildSpec();
			if (buildSpec != null) {
				choices.addAll(buildSpec.getJobMap().values().stream()
						.map(it->it.getName()).collect(Collectors.toList()));
			}
			JobAware jobAware = WicketUtils.findInnermost(component, JobAware.class);
			if (jobAware != null) {
				Job job = jobAware.getJob();
				if (job != null)
					choices.remove(job.getName());
			}
		}
		return choices;
	}

	@Nullable
	public static String getToken(HttpServletRequest request) {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null)
			authHeader = request.getHeader(CloneInfo.ONEDEV_AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith(KubernetesHelper.BEARER + " "))
			return authHeader.substring(KubernetesHelper.BEARER.length() + 1);
		else
			return null;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
}
