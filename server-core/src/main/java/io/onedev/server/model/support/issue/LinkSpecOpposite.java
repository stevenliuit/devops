package io.onedev.server.model.support.issue;

import java.io.Serializable;

import javax.annotation.Nullable;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.model.Project;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.search.entity.issue.IssueQueryParseOption;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class LinkSpecOpposite implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	
	private boolean multiple;
	
	private String issueQuery;
	
	@Editable(order=100, name="另一边的名字", description="另一边的链接名称. "
			+ "例如，如果名称是<tt>子问题</tt>，另一边的名称可以是<tt>父问题</tt>")
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=200, name="多在另一边", description="是否可以在另一侧链接多个问题. 例如，另一侧的子问题意味着父问题，如果只允许一个父问题，则该侧的多个应该是错误的")
	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	@Editable(order=300, name="另一端的可链接问题", placeholder="所有问题", 
			description="（可选）指定可以在另一侧链接的问题标准")
	@io.onedev.server.web.editable.annotation.IssueQuery
	public String getIssueQuery() {
		return issueQuery;
	}

	public void setIssueQuery(String issueQuery) {
		this.issueQuery = issueQuery;
	}

	public IssueQuery getParsedIssueQuery(@Nullable Project project) {
		IssueQueryParseOption option = new IssueQueryParseOption();
		return IssueQuery.parse(project, issueQuery, option, false);
	}
	
}
