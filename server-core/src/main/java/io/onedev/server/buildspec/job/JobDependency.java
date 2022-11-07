package io.onedev.server.buildspec.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.BuildSpecAware;
import io.onedev.server.buildspec.param.spec.ParamSpec;
import io.onedev.server.buildspec.param.supply.ParamSupply;
import io.onedev.server.util.ComponentContext;
import io.onedev.server.util.EditContext;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.OmitName;
import io.onedev.server.web.editable.annotation.ParamSpecProvider;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.editable.annotation.VariableOption;
import io.onedev.server.web.util.WicketUtils;

@Editable
public class JobDependency implements Serializable {

	private static final long serialVersionUID = 1L;

	private String jobName;
	
	private boolean requireSuccessful = true;
	
	private List<ParamSupply> jobParams = new ArrayList<>();
	
	private String artifacts = "**";
	
	private String destinationPath;
	
	// change Named("jobName") also if change name of this property 
	@Editable(order=100, name="Job")
	@ChoiceProvider("getJobChoices")
	@NotEmpty
	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	@Editable(order=150, description="是否要求此依赖成功")
	public boolean isRequireSuccessful() {
		return requireSuccessful;
	}

	public void setRequireSuccessful(boolean requireSuccessful) {
		this.requireSuccessful = requireSuccessful;
	}

	@Editable(order=200, name="Job参数")
	@ParamSpecProvider("getParamSpecs")
	@VariableOption(withBuildVersion=false, withDynamicVariables=false)
	@OmitName
	public List<ParamSupply> getJobParams() {
		return jobParams;
	}

	public void setJobParams(List<ParamSupply> jobParams) {
		this.jobParams = jobParams;
	}
	
	@Editable(order=300, name="要检索的工件", placeholder="不检索", description=""
			+ "可选地指定要从依赖项中检索到的工件 "
			+ "<a href='$docRoot/pages/concepts.md#job-workspace'>工作空间</a>. "
			+ "只能检索已发布的工件（通过工件发布步骤）。 留空不检索任何工件")
	@Interpolative(variableSuggester="suggestVariables")
	@Patterns(path=true)
	public String getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(String artifacts) {
		this.artifacts = artifacts;
	}

	@Editable(order=400, placeholder="工作空间", description=""
			+ "（可选）指定相对于的路径<a href='$docRoot/pages/concepts.md#job-workspace'>工作空间</a> "
			+ "放置检索到的工件。 留空以使用作业工作区本身")
	@Interpolative(variableSuggester="suggestVariables")
	public String getDestinationPath() {
		return destinationPath;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
	@SuppressWarnings("unused")
	private static List<String> getJobChoices() {
		return Job.getChoices();
	}
	
	@SuppressWarnings("unused")
	private static List<ParamSpec> getParamSpecs() {
		String jobName = (String) EditContext.get().getInputValue("jobName");
		if (jobName != null) {
			Component component = ComponentContext.get().getComponent();
			BuildSpecAware buildSpecAware = WicketUtils.findInnermost(component, BuildSpecAware.class);
			if (buildSpecAware != null) {
				BuildSpec buildSpec = buildSpecAware.getBuildSpec();
				if (buildSpec != null) {
					Job job = buildSpec.getJobMap().get(jobName);
					if (job != null)
						return job.getParamSpecs();
				}
			}
		} 
		return new ArrayList<>();
	}

}
