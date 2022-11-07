package io.onedev.server.plugin.imports.url;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.validation.ConstraintValidatorContext;

import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.authz.UnauthorizedException;
import javax.validation.constraints.NotEmpty;

import com.google.common.base.Preconditions;

import io.onedev.commons.bootstrap.SensitiveMasker;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.commons.utils.StringUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.git.command.LsRemoteCommand;
import io.onedev.server.model.Project;
import io.onedev.server.storage.StorageManager;
import io.onedev.server.util.EditContext;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Password;

@Editable
@ClassValidating
public class ImportServer implements Serializable, Validatable {

	private static final long serialVersionUID = 1L;
	
	private String url;
	
	private Authentication authentication;
	
	private String project;
	
	@Editable(order=5, name="URL", description="指定远程 git 存储库的 URL。 仅支持 http/https 协议")
	@NotEmpty
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Editable(order=10, name="需要身份验证")
	public Authentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	@Editable(order=200, placeholderProvider="getProjectPlaceholder", 
			description="指定要在系统端创建的项目")
	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	@SuppressWarnings("unused")
	private static String getProjectPlaceholder() {
		String url = (String) EditContext.get().getInputValue("url");
		return deriveProjectPath(url);
	}
	
	@Nullable
	private static String deriveProjectPath(String url) {
		String hostAndPath = StringUtils.substringAfter(url, "//");
		if (StringUtils.isNotBlank(hostAndPath)) {
			String path = StringUtils.stripEnd(StringUtils.substringAfter(hostAndPath, "/"), "/");
			if (StringUtils.isNotBlank(path))
				return path;
		}
		return null;
	}
	
	String importProject(boolean dryRun, TaskLogger logger) {
		Long projectId = null;
		
		try {
			logger.log("Cloning code from " + getUrl() + "...");

			String projectPath = getProject();
			if (projectPath == null)
				projectPath = deriveProjectPath(getUrl());
			if (projectPath == null)
				throw new ExplicitException("Invalid url: " + getUrl());
			
			Project project = getProjectManager().initialize(projectPath);
			Preconditions.checkState(project.isNew());				
			
			URIBuilder builder = new URIBuilder(getUrl());
			if (authentication != null)
				builder.setUserInfo(authentication.getUserName(), authentication.getPassword());
			
			SensitiveMasker.push(new SensitiveMasker() {

				@Override
				public String mask(String text) {
					if (authentication != null)
						return StringUtils.replace(text, authentication.getPassword(), "******");
					else
						return text;
				}
				
			});
			try {
				if (dryRun) {
					new LsRemoteCommand().remote(builder.build().toString()).refs("HEAD").quiet(true).call();
				} else {
					getProjectManager().clone(project, builder.build().toString());
					projectId = project.getId();
				}
			} finally {
				SensitiveMasker.pop();
			}
			
			return "项目导入成功";
		} catch (Exception e) {
			if (projectId != null)
				OneDev.getInstance(StorageManager.class).deleteProjectDir(projectId);
			throw new RuntimeException(e);
		} 	
	}	
	
	private ProjectManager getProjectManager() {
		return OneDev.getInstance(ProjectManager.class);
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean isValid = true;
		
		if (getUrl() != null) {
			if (!getUrl().startsWith("http://") && !getUrl().startsWith("https://")) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("仅支持 http(s) 协议")
						.addPropertyNode("url").addConstraintViolation();
			}
		}
		
		if (getProject() != null) {
			String errorMessage = null;
			try {
				Project project = getProjectManager().initialize(getProject());
				if (!project.isNew()) 
					errorMessage = "项目已经存在";
			} catch (UnauthorizedException e) {
				errorMessage = e.getMessage();
			}
			if (errorMessage != null) {
				context.buildConstraintViolationWithTemplate(errorMessage)
						.addPropertyNode("project").addConstraintViolation();
				isValid = false;
			}
		}
		
		if (!isValid)
			context.disableDefaultConstraintViolation();
		
		return isValid;
	}

	@Editable
	public static class Authentication implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private String userName;
		
		private String password;

		@Editable(order=100)
		@NotEmpty
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		@Editable(order=200)
		@Password
		@NotEmpty
		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
		
	}
}
