package io.onedev.server.buildspec.job.trigger;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.buildspec.job.SubmitReason;
import io.onedev.server.git.GitUtils;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.util.match.Matcher;
import io.onedev.server.util.match.PathMatcher;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

public abstract class PullRequestTrigger extends JobTrigger {

	private static final long serialVersionUID = 1L;

	private String branches;
	
	private String paths;
	
	@Editable(name="目标分支机构", placeholder="任何分支", order=100, description=""
			+ "（可选）指定要检查的拉请求的以空格分隔的目标分支. "
			+ "使用噢 '**', '*' 或者 '?' for <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀为 '-' 以排除。保留为空以匹配所有分支")
	@Patterns(suggester = "suggestBranches", path=true)
	public String getBranches() {
		return branches;
	}

	public void setBranches(String branches) {
		this.branches = branches;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestBranches(String matchWith) {
		return SuggestionUtils.suggestBranches(Project.get(), matchWith);
	}
	
	@Editable(name="触摸的文件", order=200, placeholder="任何文件", description=""
			+ "（可选）指定要检查的以空格分隔的文件. "
			+ "使用 '**', '*' 或者 '?' 对于 <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀为 '-' 以排除。保留为空以匹配所有文件")
	@Patterns(suggester = "getPathSuggestions", path=true)
	public String getPaths() {
		return paths;
	}

	public void setPaths(String paths) {
		this.paths = paths;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> getPathSuggestions(String matchWith) {
		return SuggestionUtils.suggestBlobs(Project.get(), matchWith);
	}

	private boolean touchedFile(PullRequest request) {
		if (getPaths() != null) {
			Collection<String> changedFiles = GitUtils.getChangedFiles(request.getTargetProject().getRepository(), 
					request.getBaseCommit(), request.getLatestUpdate().getHeadCommit());
			PatternSet patternSet = PatternSet.parse(getPaths());
			Matcher matcher = new PathMatcher();
			for (String changedFile: changedFiles) {
				if (patternSet.matches(matcher, changedFile))
					return true;
			}
			return false;
		} else {
			return true;
		}
	}
	
	@Nullable
	protected SubmitReason triggerMatches(PullRequest request) {
		String targetBranch = request.getTargetBranch();
		Matcher matcher = new PathMatcher();
		if ((branches == null || PatternSet.parse(branches).matches(matcher, targetBranch)) 
				&& touchedFile(request)) {
			return new SubmitReason() {

				@Override
				public String getRefName() {
					return request.getMergeRef();
				}

				@Override
				public PullRequest getPullRequest() {
					return request;
				}

				@Override
				public String getDescription() {
					return "Pull请求 #" + request.getNumber() + " 已打开/更新";
				}
				
			};
		}
		return null;
	}
	
	protected String getTriggerDescription(String action) {
		String description;
		if (getBranches() != null && getPaths() != null)
			description = String.format("当 " + action + " 针对分支 '%s' and touching files '%s'", getBranches(), getPaths());
		else if (getBranches() != null)
			description = String.format("当 " + action + " 针对分支 '%s'", getBranches());
		else if (getPaths() != null)
			description = String.format("当 " + action + " 拉取请求触摸文件 '%s'", getPaths());
		else
			description = "当 " + action + " 拉取请求";
		return description;
	}

}
