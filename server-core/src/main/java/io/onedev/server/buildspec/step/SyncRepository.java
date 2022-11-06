package io.onedev.server.buildspec.step;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.SystemUtils;
import javax.validation.constraints.NotEmpty;

import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.utils.StringUtils;
import io.onedev.commons.utils.command.Commandline;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.git.config.GitConfig;
import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable
@ClassValidating
public abstract class SyncRepository extends ServerSideStep implements Validatable {

	private static final long serialVersionUID = 1L;

	private String remoteUrl;
	
	private String userName;
	
	private String passwordSecret;
	
	private boolean withLfs;
	
	private boolean force;

	@Editable(order=100, name="远程URL", description="指定远程 git 存储库的 URL. "
			+ "仅支持 http/https 协议")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getRemoteUrl() {
		return remoteUrl;
	}

	@Editable(order=200)
	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
	@Editable(order=300, description="可选择指定用户名以访问上述存储库")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Editable(order=400, name="密码/访问令牌", 
			description="指定一个秘密用作密码或访问令牌以访问上述存储库")
	@ChoiceProvider("getPasswordSecretChoices")
	public String getPasswordSecret() {
		return passwordSecret;
	}

	public void setPasswordSecret(String passwordSecret) {
		this.passwordSecret = passwordSecret;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getPasswordSecretChoices() {
		return Project.get().getHierarchyJobSecrets()
				.stream().map(it->it.getName()).collect(Collectors.toList());
	}

	@Editable(order=450, name="传输 Git LFS 文件", descriptionProvider="getLfsDescription")
	public boolean isWithLfs() {
		return withLfs;
	}

	public void setWithLfs(boolean withLfs) {
		this.withLfs = withLfs;
	}
	
	@SuppressWarnings("unused")
	private static String getLfsDescription() {
		if (!Bootstrap.isInDocker()) {
			return "如果启用此选项，则需要在系统服务器上安装 git lfs 命令 "
					+ "(即使这一步在其他节点上运行)";
		} else {
			return null;
		}
	}

	@Editable(order=500, description="是否使用 force 选项覆盖更改，以防 ref 更新无法快进")
	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public String getRemoteUrlWithCredential(Build build) {
		String encodedPassword = null;
		if (getPasswordSecret() != null) {
			try {
				String password = build.getJobSecretAuthorizationContext().getSecretValue(getPasswordSecret());
				encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		String protocol = StringUtils.substringBefore(getRemoteUrl(), "//");
		String hostAndPath = StringUtils.substringAfter(getRemoteUrl(), "//");
		
		String remoteUrlWithCredentials = protocol + "//";
		
		if (getUserName() != null && encodedPassword != null)
			remoteUrlWithCredentials += getUserName() + ":" + encodedPassword + "@" + hostAndPath;
		else if (getUserName() != null)
			remoteUrlWithCredentials += getUserName() + "@" + hostAndPath;
		else if (encodedPassword != null)
			remoteUrlWithCredentials += encodedPassword + "@" + hostAndPath;
		else
			remoteUrlWithCredentials += hostAndPath;
		
		return remoteUrlWithCredentials;
	}
	
	protected Commandline newGit(Project project) {
		Commandline git = new Commandline(OneDev.getInstance(GitConfig.class).getExecutable());
		if (SystemUtils.IS_OS_MAC_OSX) {
			String path = System.getenv("PATH") + ":/usr/local/bin";
			git.environments().put("PATH", path);
		}
		git.workingDir(project.getGitDir());
		return git;
	}
	
	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean isValid = true;
		if (getRemoteUrl() != null) {
			if (!getRemoteUrl().startsWith("http://") && !getRemoteUrl().startsWith("https://")) {
				isValid = false;
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate("仅支持 http(s) 协议")
						.addPropertyNode("remoteUrl").addConstraintViolation();
			}
		}
		return isValid;
	}

}
