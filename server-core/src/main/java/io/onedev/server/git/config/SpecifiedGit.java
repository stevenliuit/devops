package io.onedev.server.git.config;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.OmitName;

@Editable(name="使用指定的 Git", order=200)
public class SpecifiedGit extends GitConfig {

	private static final long serialVersionUID = 1L;
	
	private String gitPath;
	
	@Editable(description="指定 git 可执行文件的路径，例如: <tt>/usr/bin/git</tt>")
	@OmitName
	@NotEmpty
	public String getGitPath() {
		return gitPath;
	}

	public void setGitPath(String gitPath) {
		this.gitPath = gitPath;
	}

	@Override
	public String getExecutable() {
		return gitPath;
	}

}
