package io.onedev.server.model.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.Valid;

import org.eclipse.jgit.lib.ObjectId;
import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Build.Status;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.util.match.PathMatcher;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.util.reviewrequirement.ReviewRequirement;
import io.onedev.server.util.usage.Usage;
import io.onedev.server.util.usermatch.Anyone;
import io.onedev.server.util.usermatch.UserMatch;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Horizontal;
import io.onedev.server.web.editable.annotation.JobChoice;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
@Horizontal
public class BranchProtection implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean enabled = true;
	
	private String branches;
	
	private String userMatch = new Anyone().toString();
	
	private boolean preventForcedPush = true;
	
	private boolean preventDeletion = true;
	
	private boolean preventCreation = true;
	
	private boolean signatureRequired = false;
	
	private String reviewRequirement;
	
	private transient ReviewRequirement parsedReviewRequirement;
	
	private List<String> jobNames = new ArrayList<>();
	
	private List<FileProtection> fileProtections = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Editable(order=100, description="指定要保护的以空格分隔的分支. 使用 '**', '*' 或者 '?' for <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀 '-' 排除")
	@Patterns(suggester = "suggestBranches", path=true)
	@NotEmpty
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
	
	@Editable(order=150, name="适用用户", description="仅当更改分支的用户符合此处指定的条件时，才会应用规则")
	@io.onedev.server.web.editable.annotation.UserMatch
	@NotEmpty(message="不能为空")
	public String getUserMatch() {
		return userMatch;
	}

	public void setUserMatch(String userMatch) {
		this.userMatch = userMatch;
	}

	@Editable(order=200, description="检查此项以防止强制推送")
	public boolean isPreventForcedPush() {
		return preventForcedPush;
	}

	public void setPreventForcedPush(boolean preventForcedPush) {
		this.preventForcedPush = preventForcedPush;
	}

	@Editable(order=300, description="选中此项以防止分支删除")
	public boolean isPreventDeletion() {
		return preventDeletion;
	}

	public void setPreventDeletion(boolean preventDeletion) {
		this.preventDeletion = preventDeletion;
	}

	@Editable(order=350, description="选中此项以防止创建分支")
	public boolean isPreventCreation() {
		return preventCreation;
	}

	public void setPreventCreation(boolean preventCreation) {
		this.preventCreation = preventCreation;
	}

	@Editable(order=360, description="选中此项以要求头部提交的有效签名")
	public boolean isSignatureRequired() {
		return signatureRequired;
	}

	public void setSignatureRequired(boolean signatureRequired) {
		this.signatureRequired = signatureRequired;
	}

	@Editable(order=400, name="所需的审稿人", placeholder="没有人", description="（可选）为指定分支的更改指定所需的审阅者")
	@io.onedev.server.web.editable.annotation.ReviewRequirement
	public String getReviewRequirement() {
		return reviewRequirement;
	}

	public void setReviewRequirement(String reviewRequirement) {
		this.reviewRequirement = reviewRequirement;
	}

	public ReviewRequirement getParsedReviewRequirement() {
		if (parsedReviewRequirement == null)
			parsedReviewRequirement = ReviewRequirement.parse(reviewRequirement, true);
		return parsedReviewRequirement;
	}
	
	public void setParsedReviewRequirement(ReviewRequirement parsedReviewRequirement) {
		this.parsedReviewRequirement = parsedReviewRequirement;
		reviewRequirement = parsedReviewRequirement.toString();
	}
	
	@Editable(order=500, name="所需的构建", placeholder="没有任何", description="（可选）选择所需的构建")
	@JobChoice(tagsMode=true)
	public List<String> getJobNames() {
		return jobNames;
	}

	public void setJobNames(List<String> jobNames) {
		this.jobNames = jobNames;
	}
	
	@Editable(order=700, description="可选择指定路径保护规则")
	@Valid
	public List<FileProtection> getFileProtections() {
		return fileProtections;
	}

	public void setFileProtections(List<FileProtection> fileProtections) {
		this.fileProtections = fileProtections;
	}
	
	public FileProtection getFileProtection(String file) {
		Set<String> jobNames = new HashSet<>();
		ReviewRequirement reviewRequirement = ReviewRequirement.parse(null, true);
		for (FileProtection protection: fileProtections) {
			if (PatternSet.parse(protection.getPaths()).matches(new PathMatcher(), file)) {
				jobNames.addAll(protection.getJobNames());
				reviewRequirement.mergeWith(protection.getParsedReviewRequirement());
			}
		}
		FileProtection protection = new FileProtection();
		protection.setJobNames(new ArrayList<>(jobNames));
		protection.setParsedReviewRequirement(reviewRequirement);
		return protection;
	}
	
	public void onRenameGroup(String oldName, String newName) {
		userMatch = UserMatch.onRenameGroup(userMatch, oldName, newName);
		reviewRequirement = ReviewRequirement.onRenameGroup(reviewRequirement, oldName, newName);
		
		for (FileProtection fileProtection: getFileProtections()) {
			fileProtection.setReviewRequirement(ReviewRequirement.onRenameGroup(
					fileProtection.getReviewRequirement(), oldName, newName));
		}
	}
	
	public Usage onDeleteGroup(String groupName) {
		Usage usage = new Usage();
		if (UserMatch.isUsingGroup(userMatch, groupName))
			usage.add("applicable users");
		if (ReviewRequirement.isUsingGroup(reviewRequirement, groupName))
			usage.add("required reviewers");

		for (FileProtection protection: getFileProtections()) {
			if (ReviewRequirement.isUsingGroup(protection.getReviewRequirement(), groupName)) {
				usage.add("file protections");
				break;
			}
		}
		return usage.prefix("分支保护 '" + getBranches() + "'");
	}
	
	public void onRenameUser(String oldName, String newName) {
		userMatch = UserMatch.onRenameUser(userMatch, oldName, newName);
		reviewRequirement = ReviewRequirement.onRenameUser(reviewRequirement, oldName, newName);
		
		for (FileProtection fileProtection: getFileProtections()) {
			fileProtection.setReviewRequirement(ReviewRequirement.onRenameUser(
					fileProtection.getReviewRequirement(), oldName, newName));
		}	
	}
	
	public Usage onDeleteUser(String userName) {
		Usage usage = new Usage();
		if (UserMatch.isUsingUser(userMatch, userName))
			usage.add("适用用户");
		if (ReviewRequirement.isUsingUser(reviewRequirement, userName))
			usage.add("要求的审稿人");

		for (FileProtection protection: getFileProtections()) {
			if (ReviewRequirement.isUsingUser(protection.getReviewRequirement(), userName)) {
				usage.add("文件保护");
				break;
			}
		}
		return usage.prefix("分支保护 '" + getBranches() + "'");
	}
	
	/**
	 * Check if specified user can modify specified file in specified branch.
	 *
	 * @param user
	 * 			user to be checked
	 * @param branch
	 * 			branch to be checked
	 * @param file
	 * 			file to be checked
	 * @return
	 * 			result of the check. 
	 */
	public boolean isReviewRequiredForModification(User user, Project project, 
			String branch, @Nullable String file) {
		ReviewRequirement requirement = getParsedReviewRequirement();
		if (!requirement.getUsers().isEmpty() || !requirement.getGroups().isEmpty()) 
			return true;
		
		if (file != null) {
			requirement = getFileProtection(file).getParsedReviewRequirement();
			return !requirement.getUsers().isEmpty() || !requirement.getGroups().isEmpty();
		} 
		
		return false;
	}
	
	public boolean isBuildRequiredForModification(Project project, String branch, @Nullable String file) {
		return !getJobNames().isEmpty() || file != null && !getFileProtection(file).getJobNames().isEmpty();
	}

	/**
	 * Check if specified user can push specified commit to specified ref.
	 *
	 * @param user
	 * 			user to be checked
	 * @param branchName
	 * 			branchName to be checked
	 * @param oldObjectId
	 * 			old object id of the ref
	 * @param newObjectId
	 * 			new object id of the ref
	 * @param gitEnvs
	 * 			git environments
	 * @return
	 * 			result of the check
	 */
	public boolean isReviewRequiredForPush(User user, Project project, String branch, ObjectId oldObjectId, 
			ObjectId newObjectId, Map<String, String> gitEnvs) {
		ReviewRequirement requirement = getParsedReviewRequirement();
		if (!requirement.getUsers().isEmpty() || !requirement.getGroups().isEmpty()) 
			return true;
		
		for (String changedFile: project.getChangedFiles(oldObjectId, newObjectId, gitEnvs)) {
			requirement = getFileProtection(changedFile).getParsedReviewRequirement();
			if (!requirement.getUsers().isEmpty() || !requirement.getGroups().isEmpty())
				return true;
		}

		return false;
	}

	public Collection<String> getRequiredJobs(Project project, ObjectId oldObjectId, ObjectId newObjectId, 
			Map<String, String> gitEnvs) {
		Collection<String> requiredJobs = new HashSet<>(getJobNames());
		for (String changedFile: project.getChangedFiles(oldObjectId, newObjectId, gitEnvs)) 
			requiredJobs.addAll(getFileProtection(changedFile).getJobNames());
		return requiredJobs;
	}
	
	public boolean isBuildRequiredForPush(Project project, ObjectId oldObjectId, ObjectId newObjectId, 
			Map<String, String> gitEnvs) {
		Collection<String> requiredJobNames = getRequiredJobs(project, oldObjectId, newObjectId, gitEnvs);

		Collection<Build> builds = OneDev.getInstance(BuildManager.class).query(project, newObjectId, null);
		for (Build build: builds) {
			if (requiredJobNames.contains(build.getJobName()) && build.getStatus() != Status.SUCCESSFUL)
				return true;
		}
		requiredJobNames.removeAll(builds.stream().map(it->it.getJobName()).collect(Collectors.toSet()));
		return !requiredJobNames.isEmpty();			
	}

}
