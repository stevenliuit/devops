package io.onedev.server.web.page.project.setting.general;

import java.io.Serializable;

import javax.annotation.Nullable;

import io.onedev.server.util.validation.annotation.ProjectPath;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.ParentChoice;

@Editable
public class ParentBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String parentPath;

	@Editable(name="父项目", placeholder="没有父项目", description="设置和权限 "
			+ "的父项目将被此项目继承")
	@ProjectPath
	@ParentChoice
	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(@Nullable String parentPath) {
		this.parentPath = parentPath;
	}
	
}
