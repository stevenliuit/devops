package io.onedev.server.plugin.authenticator.ldap;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.Editable;

@Editable(name="活动目录", order=100)
public class ActiveDirectoryAuthenticator extends LdapAuthenticator {

	private static final long serialVersionUID = 1L;

	private String groupSearchBase;
	
    @Editable(order=100, name="LDAP URL", description=
    	"指定 Active Directory 服务器的 LDAP URL，例如: <i>ldap://ad-server</i>, or <i>ldaps://ad-server</i>")
    @NotEmpty
	@Override
	public String getLdapUrl() {
		return super.getLdapUrl();
	}

	@Override
	public void setLdapUrl(String ldapUrl) {
		super.setLdapUrl(ldapUrl);
	}

	@Editable(order=300, description=""
			+ "针对 Active Directory 对用户进行身份验证并检索关联的属性和组, "
			+ "必须首先针对 Active Directory 服务器对自身进行身份验证，而系统通过 "
			+ "<i>&lt;帐户名称&gt;@&lt;域名&gt;</i>, 例如: <i>onedev@example.com</i>")
	@NotEmpty
	@Override
	public String getManagerDN() {
		return super.getManagerDN();
	}

	@Override
	public void setManagerDN(String managerDN) {
		super.setManagerDN(managerDN);
	}

	@Editable(order=500, description=
		"指定用户搜索的基节点. 例如: <i>cn=Users, dc=example, dc=com</i>")
	@NotEmpty
	@Override
	public String getUserSearchBase() {
		return super.getUserSearchBase();
	}

	@Override
	public void setUserSearchBase(String userSearchBase) {
		super.setUserSearchBase(userSearchBase);
	}

	@Override
	public String getUserSearchFilter() {
		return "(&(sAMAccountName={0})(objectclass=user))";
	}
    
	@Override
	public void setUserSearchFilter(String userSearchFilter) {
		super.setUserSearchFilter(userSearchFilter);
	}

	@Editable(order=1000, placeholder="不检索组", description=""
			+ "如果要检索组成员身份信息，可以选择指定组搜索基础 "
			+ "的用户. 例如: <i>cn=Users, dc=example, dc=com</i>. 给予适当的 "
			+ "对Active Directory组的权限, 应定义具有相同名称的系统组. "
			+ "保留为空以管理系统端的组成员身份")
	public String getGroupSearchBase() {
		return groupSearchBase;
	}

	public void setGroupSearchBase(String groupSearchBase) {
		this.groupSearchBase = groupSearchBase;
	}

	@Override
	public GroupRetrieval getGroupRetrieval() {
		if (getGroupSearchBase() != null) {
			SearchGroupsUsingFilter groupRetrieval = new SearchGroupsUsingFilter();
			groupRetrieval.setGroupSearchBase(getGroupSearchBase());
			groupRetrieval.setGroupSearchFilter("(&(member:1.2.840.113556.1.4.1941:={0})(objectclass=group))");
			return groupRetrieval;
		} else {
			return new DoNotRetrieveGroups();
		}
	}

}
