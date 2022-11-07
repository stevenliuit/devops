package io.onedev.server.buildspec.job.projectdependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import javax.validation.constraints.NotEmpty;

import edu.emory.mathcs.backport.java.util.Collections;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.Project;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.security.permission.AccessProject;
import io.onedev.server.util.EditContext;
import io.onedev.server.util.facade.ProjectCache;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.util.WicketUtils;

@Editable
public class ProjectDependency implements Serializable {

	private static final long serialVersionUID = 1L;

	private String projectPath;

	private BuildProvider buildProvider = new LastFinishedBuild();
	
	private String artifacts = "**";
	
	private String destinationPath;
	
	private String accessTokenSecret;
	
	// change Named("projectPath") also if change name of this property 
	@Editable(order=200, name="项目", description="指定要从中检索工件的项目")
	@ChoiceProvider("getProjectChoices")
	@NotEmpty
	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

	@SuppressWarnings("unused")
	private static List<String> getProjectChoices() {
		List<String> choices = new ArrayList<>();
		Project currentProject = ((ProjectPage)WicketUtils.getPage()).getProject();
		
		ProjectManager projectManager = OneDev.getInstance(ProjectManager.class);
		ProjectCache cache = projectManager.cloneCache();
		for (Project project: projectManager.getPermittedProjects(new AccessProject())) {
			if (!project.equals(currentProject))
				choices.add(cache.get(project.getId()).getPath());
		}
		
		Collections.sort(choices);
		
		return choices;
	}

	@Editable(order=300, name="编译")
	@NotNull
	public BuildProvider getBuildProvider() {
		return buildProvider;
	}

	public void setBuildProvider(BuildProvider buildProvider) {
		this.buildProvider = buildProvider;
	}

	@Nullable
	static Project getInputProject(EditContext editContext) {
		String projectPath = (String) editContext.getInputValue("projectPath");
		if (projectPath != null) {
			Project project = OneDev.getInstance(ProjectManager.class).findByPath(projectPath);
			if (project != null && SecurityUtils.canReadCode(project))
				return project;
		}
		return null;
	}
	
	@Editable(order=400, name="要检索的工件", description="指定要检索到 <a href='$docRoot/pages/concepts.md#job-workspace'>工作空间</a>. "
			+ "只能检索已发布的工件（通过工件发布步骤）.")
	@Interpolative(variableSuggester="suggestVariables")
	@Patterns(path=true)
	@NotEmpty
	public String getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(String artifacts) {
		this.artifacts = artifacts;
	}
	
	@Editable(order=500, placeholder="工作空间", description=""
			+ "（可选）指定相对于的路径<a href='$docRoot/pages/concepts.md#job-workspace'>工作空间</a> "
			+ "放置检索到的工件。保留为空以使用作业工作区本身")
	@Interpolative(variableSuggester="suggestVariables")
	public String getDestinationPath() {
		return destinationPath;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
	@Editable(order=500, placeholder="匿名访问", description="指定要用作访问令牌的机密，以从上述项目中检索工件。如果未指定，将匿名访问项目工件")
	@ChoiceProvider("getAccessTokenSecretChoices")
	@Nullable
	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getAccessTokenSecretChoices() {
		return Project.get().getHierarchyJobSecrets()
				.stream().map(it->it.getName()).collect(Collectors.toList());
	}
	
}
