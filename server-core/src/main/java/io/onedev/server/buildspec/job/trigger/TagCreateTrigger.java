package io.onedev.server.buildspec.job.trigger;

import java.util.List;

import org.eclipse.jgit.lib.ObjectId;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.buildspec.job.Job;
import io.onedev.server.buildspec.job.SubmitReason;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.RefUpdated;
import io.onedev.server.git.GitUtils;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.util.match.PathMatcher;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable(order=200, name="标记创建")
public class TagCreateTrigger extends JobTrigger {

	private static final long serialVersionUID = 1L;

	private String tags;
	
	private String branches;
	
	@Editable(name="标签", order=100, placeholder="任何tag", description=""
			+ "（可选）指定要检查的以空格分隔的标记. 使用 '**', '*' 或者 '?' 对于 "
			+ "<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀为 '-' 以排除。保留为空以匹配所有标记")
	@Patterns(suggester="suggestTags", path=true)
	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestTags(String matchWith) {
		return SuggestionUtils.suggestTags(Project.get(), matchWith);
	}

	@Editable(name="在分支上", order=200, placeholder="任何分支", description=""
			+ "仅当标记的提交在此处指定的分支上时，此触发器才适用. "
			+ "多个分支应使用空格分隔. 使用 '**', '*' 或者 '?' 对于 "
			+ "<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀为 '-' 以排除。保留为空以匹配所有分支")
	@Patterns(suggester="suggestBranches", path=true)
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
	
	@Override
	public SubmitReason triggerMatches(ProjectEvent event, Job job) {
		if (event instanceof RefUpdated) {
			RefUpdated refUpdated = (RefUpdated) event;
			String updatedTag = GitUtils.ref2tag(refUpdated.getRefName());
			ObjectId commitId = refUpdated.getNewCommitId();
			Project project = event.getProject();
			if (updatedTag != null && !commitId.equals(ObjectId.zeroId()) 
					&& (tags == null || PatternSet.parse(tags).matches(new PathMatcher(), updatedTag))
					&& (branches == null || project.isCommitOnBranches(commitId, branches))) {
				return new SubmitReason() {

					@Override
					public String getRefName() {
						return refUpdated.getRefName();
					}

					@Override
					public PullRequest getPullRequest() {
						return null;
					}

					@Override
					public String getDescription() {
						return "Tag '" + updatedTag + "' 已创建";
					}
					
				};
			}
		}
		return null;
	}

	@Override
	public String getTriggerDescription() {
		String description = "创建Tag时";
		if (tags != null)
			description += " '" + tags + "'";
		if (branches != null)
			description += " 在分支上 '" + branches + "'";
		return description;
	}

}
