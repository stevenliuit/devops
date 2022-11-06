package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.GroupManager;
import io.onedev.server.model.Group;
import io.onedev.server.util.usage.Usage;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.GroupChoice;

@Editable
public class SecuritySetting implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(SecuritySetting.class);

	private boolean enableAnonymousAccess = false;
	
	private boolean enableSelfRegister = true;
	
	private String defaultLoginGroupName;
	
	private boolean enforce2FA;
	
	private transient Optional<Group> defaultLoginGroup;
	
	@Editable(order=100, description="是否允许匿名用户访问此服务器")
	public boolean isEnableAnonymousAccess() {
		return enableAnonymousAccess;
	}

	public void setEnableAnonymousAccess(boolean enableAnonymousAccess) {
		this.enableAnonymousAccess = enableAnonymousAccess;
	}

	@Editable(order=200, name="启用用户注册", description="如果启用此选项，用户可以注册")
	public boolean isEnableSelfRegister() {
		return enableSelfRegister;
	}

	public void setEnableSelfRegister(boolean enableSelfRegister) {
		this.enableSelfRegister = enableSelfRegister;
	}

	@Editable(order=300, name="默认登录组", description="（可选）指定默认组 "
			+ "对于所有登录的用户")
	@GroupChoice
	public String getDefaultLoginGroupName() {
		return defaultLoginGroupName;
	}

	public void setDefaultLoginGroupName(String defaultLoginGroupName) {
		this.defaultLoginGroupName = defaultLoginGroupName;
	}

	@Editable(order=400, name="实施双因素身份验证", description="选中此项以强制执行 "
			+ "所有用户在下次登录时设置双重身份验证。 用户将无法 "
			+ "如果设置了此选项，则自行禁用双因素身份验证")
	public boolean isEnforce2FA() {
		return enforce2FA;
	}

	public void setEnforce2FA(boolean enforce2FA) {
		this.enforce2FA = enforce2FA;
	}

	@Nullable
	public Group getDefaultLoginGroup() {
		if (defaultLoginGroup == null) {
			if (defaultLoginGroupName != null) {
	       		Group group = OneDev.getInstance(GroupManager.class).find(defaultLoginGroupName);
	       		if (group == null) 
	       			logger.error("Unable to find default login group: " + defaultLoginGroupName);
	       		defaultLoginGroup = Optional.ofNullable(group);
			} else {
				defaultLoginGroup = Optional.empty();
			}
		}
		return defaultLoginGroup.orElse(null);
	}
	
	public void onRenameGroup(String oldName, String newName) {
		if (oldName.equals(defaultLoginGroupName))
			defaultLoginGroupName = newName;
	}
	
	public Usage onDeleteGroup(String groupName) {
		Usage usage = new Usage();
		if (groupName.equals(defaultLoginGroupName))
			usage.add("default group for sign-in users");
		return usage.prefix("security setting");
	}
	
}
