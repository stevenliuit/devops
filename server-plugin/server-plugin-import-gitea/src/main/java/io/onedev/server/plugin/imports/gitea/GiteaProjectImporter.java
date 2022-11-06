package io.onedev.server.plugin.imports.gitea;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.imports.ProjectImporter;
import io.onedev.server.web.util.ImportStep;

public class GiteaProjectImporter implements ProjectImporter {

	private static final long serialVersionUID = 1L;
	
	private final ImportStep<ImportServer> serverStep = new ImportStep<ImportServer>() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getTitle() {
			return "验证 Gitea";
		}

		@Override
		protected ImportServer newSetting() {
			return new ImportServer();
		}
		
	};
	
	private final ImportStep<ImportOrganization> organizationStep = new ImportStep<ImportOrganization>() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getTitle() {
			return "选择组织";
		}

		@Override
		protected ImportOrganization newSetting() {
			ImportOrganization organization = new ImportOrganization();
			organization.server = serverStep.getSetting();
			return organization;
		}
		
	};
	
	private final ImportStep<ImportRepositories> repositoriesStep = new ImportStep<ImportRepositories>() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getTitle() {
			return "指定存储库";
		}

		@Override
		protected ImportRepositories newSetting() {
			ImportRepositories repositories = new ImportRepositories();
			String organization = organizationStep.getSetting().getOrganization();
			for (String repository: serverStep.getSetting().listRepositories(organization)) {
				ProjectMapping projectMapping = new ProjectMapping();
				projectMapping.setGiteaRepo(repository);
				projectMapping.setOneDevProject(repository);
				repositories.getProjectMappings().add(projectMapping);
			}
			return repositories;
		}
		
	};
	
	private final ImportStep<ProjectImportOption> optionStep = new ImportStep<ProjectImportOption>() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getTitle() {
			return "指定导入选项";
		}

		@Override
		protected ProjectImportOption newSetting() {
			ProjectImportOption option = new ProjectImportOption();
			List<String> giteaRepos = repositoriesStep.getSetting().getProjectMappings().stream()
					.map(it->it.getGiteaRepo()).collect(Collectors.toList());
			option.setIssueImportOption(serverStep.getSetting().buildIssueImportOption(giteaRepos));
			return option;
		}
		
	};
	
	@Override
	public String getName() {
		return GiteaPluginModule.NAME;
	}

	@Override
	public String doImport(boolean dryRun, TaskLogger logger) {
		ImportRepositories repositories = repositoriesStep.getSetting();
		ProjectImportOption option = optionStep.getSetting();
		return serverStep.getSetting().importProjects(repositories, option, dryRun, logger);
	}

	@Override
	public List<ImportStep<? extends Serializable>> getSteps() {
		return Lists.newArrayList(serverStep, organizationStep, repositoriesStep, optionStep);
	}

}
