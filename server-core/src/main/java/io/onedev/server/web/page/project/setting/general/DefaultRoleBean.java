package io.onedev.server.web.page.project.setting.general;

import java.io.Serializable;

import javax.annotation.Nullable;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.RoleManager;
import io.onedev.server.model.Role;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.RoleChoice;

@Editable
public class DefaultRoleBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String roleName;

	@Editable(name="默认角色", placeholder="没有默认角色", description="默认角色决定默认 "
			+ "授予系统中每个人的权限. 权限是项目的默认权限, "
			+ "如果它包含在项目层次结构的任何默认角色中（当前和所有祖先项目）")
	@RoleChoice
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	
	public void setRole(@Nullable Role role) {
		if (role != null)
			roleName = role.getName();
		else
			roleName = null;
	}
	
	@Nullable
	public Role getRole() {
		if (roleName != null)
			return OneDev.getInstance(RoleManager.class).find(roleName);
		else
			return null;
	}
	
}
