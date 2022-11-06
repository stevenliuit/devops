package io.onedev.server.plugin.authenticator.ldap;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.Editable;

@Editable(order=200)
public class GetGroupsUsingAttribute implements GroupRetrieval {

	private static final long serialVersionUID = 1L;
	
	private String userGroupsAttribute;

	private String groupNameAttribute = "cn";

	@Editable(order=100, description=""
			+ "指定用户LDAP条目内属性的名称，其值包含 "
			+ "所属组. 例如，一些LDAP服务器使用属性 <i>memberOf</i> 要列出组")
    @NotEmpty
	public String getUserGroupsAttribute() {
		return userGroupsAttribute;
	}

	public void setUserGroupsAttribute(String userGroupsAttribute) {
		this.userGroupsAttribute = userGroupsAttribute;
	}
	
	@Editable(order=200, description=""
			+ "指定在找到的组LDAP条目中包含组名的属性。此属性的值 "
			+ "将映射到系统组. 此属性通常设置为 <i>cn</i>")
	@NotEmpty
	public String getGroupNameAttribute() {
		return groupNameAttribute;
	}

	public void setGroupNameAttribute(String groupNameAttribute) {
		this.groupNameAttribute = groupNameAttribute;
	}
		
}
