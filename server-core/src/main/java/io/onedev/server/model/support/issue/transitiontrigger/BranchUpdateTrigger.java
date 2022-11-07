package io.onedev.server.model.support.issue.transitiontrigger;

import java.util.ArrayList;
import java.util.List;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.search.entity.issue.IssueQueryLexer;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.IssueQuery;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable(order=500, name="Code is committed")
public class BranchUpdateTrigger extends TransitionTrigger {

	private static final long serialVersionUID = 1L;

	private String branches;

	public BranchUpdateTrigger() {
		setIssueQuery(io.onedev.server.search.entity.issue.IssueQuery
				.getRuleName(IssueQueryLexer.FixedInCurrentCommit));		
	}
	
	@Editable(order=200, name="适用分支", placeholder="所有分支", description="可以选择指定适用于此触发器的空格分隔分支。 使用“**”、“*”或“？” 用于<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>。 前缀 '-' 排除。 留空以匹配所有")
	@Patterns(suggester = "suggestBranches", path=true)
	public String getBranches() {
		return branches;
	}

	public void setBranches(String branches) {
		this.branches = branches;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestBranches(String matchWith) {
		Project project = Project.get();
		if (project != null)
			return SuggestionUtils.suggestBranches(project, matchWith);
		else
			return new ArrayList<>();
	}

	@Editable(order=1000, name="适用问题", placeholder="所有", description="（可选）指定适用于此过渡的问题。 为所有问题留空")
	@IssueQuery(withOrder = false, withCurrentCommitCriteria = true)
	@Override
	public String getIssueQuery() {
		return super.getIssueQuery();
	}

	public void setIssueQuery(String issueQuery) {
		super.setIssueQuery(issueQuery);
	}
	
	@Override
	public String getDescription() {
		if (branches != null)
			return "代码提交到分支 '" + branches + "'";
		else
			return "代码提交到任何分支";
	}
	
}
