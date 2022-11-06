package io.onedev.server.buildspec.step;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.util.validation.annotation.SafePath;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable
public class VolumeMount implements Serializable {

	private static final long serialVersionUID = 1L;

	private String source;
	
	private String target;

	@Editable(order=100, description="指定相对于作业工作空间的路径以用作挂载源")
	@Interpolative(variableSuggester="suggestVariables")
	@SafePath
	@NotEmpty
	public String getSourcePath() {
		return source;
	}

	public void setSourcePath(String sourcePath) {
		this.source = sourcePath;
	}

	@Editable(order=200, description="指定容器内的路径作为挂载目标")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getTargetPath() {
		return target;
	}

	public void setTargetPath(String targetPath) {
		this.target = targetPath;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
}
