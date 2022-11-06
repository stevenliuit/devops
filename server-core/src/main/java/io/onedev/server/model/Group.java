package io.onedev.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.apache.shiro.authz.Permission;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import javax.validation.constraints.NotEmpty;

import io.onedev.server.security.permission.CreateRootProjects;
import io.onedev.server.security.permission.ProjectPermission;
import io.onedev.server.util.EditContext;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.ShowCondition;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@Editable
public class Group extends AbstractEntity implements Permission {

	private static final long serialVersionUID = 1L;
	
	public static final String PROP_ADMINISTRATOR = "administrator";

	@Column(unique=true, nullable=false)
	private String name;
	
	private String description;
	
	private boolean administrator;
	
	private boolean createRootProjects;
	
	private boolean enforce2FA;
	
	@OneToMany(mappedBy="group", cascade=CascadeType.REMOVE)
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	private Collection<GroupAuthorization> authorizations = new ArrayList<>();
	
	@OneToMany(mappedBy="group", cascade=CascadeType.REMOVE)
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	private Collection<DashboardGroupShare> dashboardShares = new ArrayList<>();
	
	@OneToMany(mappedBy="group", cascade=CascadeType.REMOVE)
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	private Collection<Membership> memberships = new ArrayList<>();
	
	private transient Collection<User> members;
	
	@Editable(order=100)
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=200, description="可选地描述组")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Editable(order=300, name="是网站管理员")
	public boolean isAdministrator() {
		return administrator;
	}

	public void setAdministrator(boolean administrator) {
		this.administrator = administrator;
	}

	@SuppressWarnings("unused")
	private static boolean isAdministratorDisabled() {
		return !(boolean) EditContext.get().getInputValue("administrator");
	}

	@Editable(order=300, name="可以创建根项目", description="是否允许创建根项目（无父项目）")
	@ShowCondition("isAdministratorDisabled")
	public boolean isCreateRootProjects() {
		return createRootProjects;
	}

	public void setCreateRootProjects(boolean createRootProjects) {
		this.createRootProjects = createRootProjects;
	}

	@Editable(order=400, name="实施双因素身份验证", description="选中此项以强制执行 "
			+ "该组中的所有用户在下次登录时设置双重身份验证。 用户 "
			+ "如果设置了此选项，则无法自行禁用双重身份验证")
	public boolean isEnforce2FA() {
		return enforce2FA;
	}

	public void setEnforce2FA(boolean enforce2FA) {
		this.enforce2FA = enforce2FA;
	}
	
	public Collection<GroupAuthorization> getAuthorizations() {
		return authorizations;
	}

	public void setAuthorizations(Collection<GroupAuthorization> authorizations) {
		this.authorizations = authorizations;
	}

	public Collection<DashboardGroupShare> getDashboardShares() {
		return dashboardShares;
	}

	public void setDashboardShares(Collection<DashboardGroupShare> dashboardShares) {
		this.dashboardShares = dashboardShares;
	}

	public Collection<Membership> getMemberships() {
		return memberships;
	}

	public void setMemberships(Collection<Membership> memberships) {
		this.memberships = memberships;
	}

	public Collection<User> getMembers() {
		if (members == null) {
			members = new HashSet<>();
			for (Membership membership: getMemberships()) {
				members.add(membership.getUser());
			}
		}
		return members;
	}

	@Override
	public int compareTo(AbstractEntity entity) {
		Group group = (Group) entity;
		return getName().compareTo(group.getName());
	}

	@Override
	public boolean implies(Permission p) {
		if (isAdministrator()) 
			return true;
		if (isCreateRootProjects() && new CreateRootProjects().implies(p))
			return true;
		if (p instanceof ProjectPermission) {
			for (GroupAuthorization authorization: getAuthorizations()) { 
				if (new ProjectPermission(authorization.getProject(), authorization.getRole()).implies(p))
					return true;
			}
		}
		return false;
	}
	
}
