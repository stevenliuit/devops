package io.onedev.server.model.support.issue.transitiontrigger;

import java.util.ArrayList;
import java.util.List;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.search.entity.issue.IssueQueryLexer;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.util.usage.Usage;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.IssueQuery;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable(order=400, name="构建成功")
public class BuildSuccessfulTrigger extends TransitionTrigger {

	private static final long serialVersionUID = 1L;
	
	private String jobNames;
	
	private String branches;
	
	public BuildSuccessfulTrigger() {
		setIssueQuery(io.onedev.server.search.entity.issue.IssueQuery
				.getRuleName(IssueQueryLexer.FixedInCurrentBuild));		
	}
	
	@Editable(order=100, name="适用Jobs", placeholder="任何job", description="可以选择指定适用于此触发器的以空格分隔的作业。 使用“*”或“？” 用于通配符匹配。 前缀 '-' 排除。 留空以匹配所有")
	@Patterns(suggester = "suggestJobs")
	public String getJobNames() {
		return jobNames;
	}

	public void setJobNames(String jobNames) {
		this.jobNames = jobNames;
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
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestJobs(String matchWith) {
		Project project = Project.get();
		if (project != null)
			return SuggestionUtils.suggest(project.getJobNames(), matchWith);
		else
			return new ArrayList<>();
	}
	
	@Editable(order=1000, name="适用问题", placeholder="所有", description="（可选）指定适用于此过渡的问题。 为所有问题留空")
	@IssueQuery(withOrder = false, withCurrentBuildCriteria = true)
	@Override
	public String getIssueQuery() {
		return super.getIssueQuery();
	}

	public void setIssueQuery(String issueQuery) {
		super.setIssueQuery(issueQuery);
	}

	@Override
	public Usage onDeleteBranch(String branchName) {
		Usage usage = super.onDeleteBranch(branchName);
		PatternSet patternSet = PatternSet.parse(getBranches());
		if (patternSet.getIncludes().contains(branchName) || patternSet.getExcludes().contains(branchName))
			usage.add("适用分支");
		return usage;
	}
	
	@Override
	public String getDescription() {
		if (jobNames != null) {
			if (branches != null)
				return "构建工作成功 '" + jobNames + "' 在分支上 '" + branches + "'";
			else
				return "构建工作成功 '" + jobNames + "' 在任何分支上";
		} else {
			if (branches != null)
				return "build 对于分支上的任何工作都是成功的 '" + branches + "'";
			else
				return "构建对于任何工作和分支都是成功的";
		}
	}
	
}
