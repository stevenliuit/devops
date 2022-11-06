package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.util.List;

import org.apache.shiro.authz.Permission;
import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.RoleManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.Role;
import io.onedev.server.util.match.PathMatcher;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.editable.annotation.RoleChoice;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
public class SenderAuthorization implements Serializable {

	private static final long serialVersionUID = 1L;

	private String senderEmails;
	
	private String authorizedProjects;
	
	private String authorizedRoleName;
	
	@Editable(order=100, name="Applicable Senders", placeholder="任何发件人", description=""
			+ "Specify space-separated sender email addresses applicable for this entry. "
			+ "Use '*' or '?' for wildcard match. Prefix with '-' to exclude. "
			+ "Leave empty to match all senders")
	@Patterns
	public String getSenderEmails() {
		return senderEmails;
	}

	public void setSenderEmails(String senderEmails) {
		this.senderEmails = senderEmails;
	}

	@Editable(order=150, placeholder="任何项目", description="指定空格分隔的项目 "
			+ "授权给以上发件人. 使用 '**' 或者 '*' 或者 '?' 为了 "
			+ "<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀 '-' 排除。 留空以授权所有项目")
	@Patterns(suggester="suggestProjects", path=true)
	public String getAuthorizedProjects() {
		return authorizedProjects;
	}

	public void setAuthorizedProjects(String authorizedProjects) {
		this.authorizedProjects = authorizedProjects;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestProjects(String matchWith) {
		return SuggestionUtils.suggestProjectPaths(matchWith);
	}
	
	@Editable(order=175, name="授权角色", description="为上述项目指定授权角色")
	@RoleChoice
	@NotEmpty
	public String getAuthorizedRoleName() {
		return authorizedRoleName;
	}

	public void setAuthorizedRoleName(String authorizedRoleName) {
		this.authorizedRoleName = authorizedRoleName;
	}
	
	public Role getAuthorizedRole() {
		Role role = OneDev.getInstance(RoleManager.class).find(authorizedRoleName);
		if (role == null)
			throw new ExplicitException("未定义的角色: " + authorizedRoleName);
		return role;
	}
	
	public boolean isPermitted(Project project, Permission privilege) {
		return (authorizedProjects == null 
					|| PatternSet.parse(authorizedProjects).matches(new PathMatcher(), project.getPath())) 
				&& getAuthorizedRole().implies(privilege);
	}
	
}