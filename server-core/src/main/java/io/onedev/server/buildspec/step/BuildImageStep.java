package io.onedev.server.buildspec.step;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.k8shelper.BuildImageFacade;
import io.onedev.k8shelper.StepFacade;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.param.ParamCombination;
import io.onedev.server.model.Build;
import io.onedev.server.util.validation.annotation.SafePath;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable(order=160, name="构建 Docker 镜像", description="构建并选择性地发布 docker 镜像. "
		+ "<span class='text-danger'>应指定注册表登录</span> 如果注册表身份验证，则在执行此步骤的作业执行程序中 "
		+ "是构建或发布所必需的")
public class BuildImageStep extends Step {

	private static final long serialVersionUID = 1L;

	private String buildPath;
	
	private String dockerfile;
	
	private String tags;
	
	private boolean publish;
	
	@Editable(order=100, description="（可选）指定相对于的构建路径 <a href='$docRoot/pages/concepts.md#job-workspace' target='_blank'>工作区</a>. "
			+ "留空以使用作业工作区本身")
	@Interpolative(variableSuggester="suggestVariables")
	@SafePath
	public String getBuildPath() {
		return buildPath;
	}

	public void setBuildPath(String buildPath) {
		this.buildPath = buildPath;
	}

	@Editable(order=200, description="可选择指定 Dockerfile 相对于 <a href='$docRoot/pages/concepts.md#job-workspace' target='_blank'>工作区</a>. "
			+ "留空以使用文件 <tt>Dockerfile</tt> 在工作工作区下")
	@Interpolative(variableSuggester="suggestVariables")
	@SafePath
	public String getDockerfile() {
		return dockerfile;
	}

	public void setDockerfile(String dockerfile) {
		this.dockerfile = dockerfile;
	}

	@Editable(order=300, description="指定image的完整标签，例如 <tt>myorg/myrepo:latest</tt>, "
			+ "<tt>myorg/myrepo:1.0.0</tt>, or <tt>myregistry:5000/myorg/myrepo:1.0.0</tt>. "
			+ "多个标签应该用空格分隔.<br>")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	@Editable(order=400, name="构建后发布", description="是否将构建的镜像发布到 docker 注册表")
	public boolean isPublish() {
		return publish;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, true, true, false);
	}
	
	@Override
	public StepFacade getFacade(Build build, String jobToken, ParamCombination paramCombination) {
		return new BuildImageFacade(getBuildPath(), getDockerfile(), getTags(), isPublish());
	}

}
