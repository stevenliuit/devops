package io.onedev.server.buildspec.step;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputCompletion;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.NamedElement;
import io.onedev.server.buildspec.param.spec.ParamSpec;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.SuggestionProvider;

@Editable
public class StepTemplate implements NamedElement, Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String PROP_STEPS = "steps";
	
	private String name;
	
	private List<Step> steps = new ArrayList<>();
	
	private List<ParamSpec> paramSpecs = new ArrayList<>();
	
	@Editable(order=100)
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
			List<String> candidates = new ArrayList<>(buildSpec.getStepTemplateMap().keySet());
			buildSpec.getStepTemplates().forEach(it->candidates.remove(it.getName()));
			return BuildSpec.suggestOverrides(candidates, status);
		}
		return new ArrayList<>();
	}
	
	@Editable(order=200, description="步骤将在同一个节点上串行执行，共享同一个 <a href='$docRoot/pages/concepts.md#job-workspace'>工作区</a>")
	@Size(min=1, max=1000, message="至少需要定义一个步骤")
	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	@Editable(order=300, name="参数规格", description="可选地定义步骤模板的参数规范")
	@Valid
	public List<ParamSpec> getParamSpecs() {
		return paramSpecs;
	}

	public void setParamSpecs(List<ParamSpec> paramSpecs) {
		this.paramSpecs = paramSpecs;
	}

}
