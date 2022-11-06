package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nullable;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import javax.validation.constraints.NotEmpty;

import com.google.common.base.Preconditions;

import io.onedev.server.OneDev;
import io.onedev.server.git.config.CurlConfig;
import io.onedev.server.git.config.GitConfig;
import io.onedev.server.git.config.SystemCurl;
import io.onedev.server.git.config.SystemGit;
import io.onedev.server.util.EditContext;
import io.onedev.server.util.ServerConfig;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
@ClassValidating
public class SystemSetting implements Serializable, Validatable {
	
	private static final long serialVersionUID = 1;
	
	private String serverUrl;
	
	private String sshRootUrl;

	private GitConfig gitConfig = new SystemGit();
	
	private CurlConfig curlConfig = new SystemCurl();
	
	private boolean gravatarEnabled;
	
	@Editable(name="Server URL", order=90, description="指定 root URL 以访问此服务器")
	@NotEmpty
	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Editable(name="SSH Root URL", order=150, placeholderProvider="getSshRootUrlPlaceholder", description=""
			+ "可选指定 SSH root URL，该 URL 将用于通过 SSH 协议构造项目克隆 url. "
			+ "留空以从服务器 url 派生")
	public String getSshRootUrl() {
		return sshRootUrl;
	}

	public void setSshRootUrl(String sshRootUrl) {
		this.sshRootUrl = sshRootUrl;
	}
	
	@SuppressWarnings("unused")
	private static String getSshRootUrlPlaceholder() {
		return deriveSshRootUrl((String) EditContext.get().getInputValue("serverUrl"));
	}

	@Nullable
	private static String deriveSshRootUrl(@Nullable String serverUrl) {
		if (serverUrl != null) {
			try {
				URL url = new URL(serverUrl);
				if (StringUtils.isNotBlank(url.getHost())) {
					if (url.getPort() == 80 || url.getPort() == 443 || url.getPort() == -1) {
						return "ssh://" + url.getHost();
					} else {
						ServerConfig serverConfig = OneDev.getInstance(ServerConfig.class);
						return "ssh://" + url.getHost() + ":" + serverConfig.getSshPort();
					}
				}
			} catch (MalformedURLException e) {
			}
		}
		return null;
	}

	@Editable(order=200, name="Git 命令行", description="管理系统需要 git 命令行来管理存储库。 最低 "
			+ "required version is 2.11.1. 如果要检索，还要确保安装了 git-lfs "
			+ "构建作业中的 LFS 文件")
	@Valid
	@NotNull(message="不能为空")
	public GitConfig getGitConfig() {
		return gitConfig;
	}

	public void setGitConfig(GitConfig gitConfig) {
		this.gitConfig = gitConfig;
	}

	@Editable(order=250, name="curl 命令行", description="管理系统配置 git hooks 以通过 curl 与自身通信")
	@Valid
	@NotNull(message="不能为空")
	public CurlConfig getCurlConfig() {
		return curlConfig;
	}

	public void setCurlConfig(CurlConfig curlConfig) {
		this.curlConfig = curlConfig;
	}
	
	@Editable(order=500, description="是否开启用户 gravatar (https://gravatar.com)")
	public boolean isGravatarEnabled() {
		return gravatarEnabled;
	}

	public void setGravatarEnabled(boolean gravatarEnabled) {
		this.gravatarEnabled = gravatarEnabled;
	}

	public String getEffectiveSshRootUrl() {
		if (getSshRootUrl() != null)
			return getSshRootUrl();
		else
			return Preconditions.checkNotNull(deriveSshRootUrl(getServerUrl()));
	}
	
    public String getSshServerName() {
    	String temp = getEffectiveSshRootUrl();
    	int index = temp.indexOf("://");
    	if (index != -1)
    		temp = temp.substring(index+3);
    	index = temp.indexOf(':');
    	if (index != -1)
    		temp = temp.substring(0, index);
    	return StringUtils.stripEnd(temp, "/\\");
    }
	
	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean isValid = true;
		if (serverUrl != null) {
			serverUrl = StringUtils.stripEnd(serverUrl, "/\\");
			try {
				URL url = new URL(serverUrl);
				if (StringUtils.isBlank(url.getProtocol())) {
					context.buildConstraintViolationWithTemplate("Protocol is not specified")
							.addPropertyNode("serverUrl").addConstraintViolation();
					isValid = false;
				} else if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
					context.buildConstraintViolationWithTemplate("Protocol should be either http or https")
							.addPropertyNode("serverUrl").addConstraintViolation();
					isValid = false;
				}
				if (StringUtils.isBlank(url.getHost())) {
					context.buildConstraintViolationWithTemplate("Host is not specified")
							.addPropertyNode("serverUrl").addConstraintViolation();
					isValid = false;
				}
				if (StringUtils.isNotBlank(url.getPath())) {
					context.buildConstraintViolationWithTemplate("Path should not be specified")
							.addPropertyNode("serverUrl").addConstraintViolation();
					isValid = false;
				}
			} catch (MalformedURLException e) {
				context.buildConstraintViolationWithTemplate("Malformed url")
						.addPropertyNode("serverUrl").addConstraintViolation();
				isValid = false;
			}
		}
		if (sshRootUrl != null) {
			sshRootUrl = StringUtils.stripEnd(sshRootUrl, "/\\");
			if (!sshRootUrl.startsWith("ssh://")) {
				context.buildConstraintViolationWithTemplate("This url should start with ssh://")
						.addPropertyNode("sshRootUrl").addConstraintViolation();
				isValid = false;
			}
		}
		
		if (!isValid)
			context.disableDefaultConstraintViolation();
		return isValid;
	}
	
}
