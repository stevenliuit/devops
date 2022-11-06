package io.onedev.server.web.page.admin.usermanagement;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Preconditions;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.model.User;
import io.onedev.server.web.component.tabbable.PageTab;
import io.onedev.server.web.component.tabbable.Tabbable;
import io.onedev.server.web.page.admin.AdministrationPage;
import io.onedev.server.web.page.admin.usermanagement.accesstoken.UserAccessTokenPage;
import io.onedev.server.web.page.admin.usermanagement.authorization.UserAuthorizationsPage;
import io.onedev.server.web.page.admin.usermanagement.avatar.UserAvatarPage;
import io.onedev.server.web.page.admin.usermanagement.emailaddresses.UserEmailAddressesPage;
import io.onedev.server.web.page.admin.usermanagement.gpgkeys.UserGpgKeysPage;
import io.onedev.server.web.page.admin.usermanagement.membership.UserMembershipsPage;
import io.onedev.server.web.page.admin.usermanagement.password.UserPasswordPage;
import io.onedev.server.web.page.admin.usermanagement.profile.UserProfilePage;
import io.onedev.server.web.page.admin.usermanagement.sshkeys.UserSshKeysPage;
import io.onedev.server.web.page.admin.usermanagement.twofactorauthentication.UserTwoFactorAuthenticationPage;

@SuppressWarnings("serial")
public abstract class UserPage extends AdministrationPage {
	
	public static final String PARAM_USER = "user";
	
	protected final IModel<User> userModel;
	
	public UserPage(PageParameters params) {
		super(params);
		
		String userIdString = params.get(PARAM_USER).toString();
		if (StringUtils.isBlank(userIdString))
			throw new RestartResponseException(UserListPage.class);
		
		Long userId = Long.valueOf(userIdString);
		Preconditions.checkArgument(userId > 0);
		
		userModel = new LoadableDetachableModel<User>() {

			@Override
			protected User load() {
				return OneDev.getInstance(UserManager.class).load(userId);
			}
			
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		List<PageTab> tabs = new ArrayList<>();
		
		tabs.add(new UserTab("帐户", "profile", UserProfilePage.class));
		tabs.add(new UserTab("电子邮件地址", "mail", UserEmailAddressesPage.class));
		tabs.add(new UserTab("编辑头像", "avatar", UserAvatarPage.class));
			
		tabs.add(new UserTab("更改密码", "password", UserPasswordPage.class));
		tabs.add(new UserTab("所属分组", "group", UserMembershipsPage.class));
		tabs.add(new UserTab("授权项目", "project", UserAuthorizationsPage.class));
		tabs.add(new UserTab("SSH Keys", "key", UserSshKeysPage.class));
		tabs.add(new UserTab("GPG Keys", "key", UserGpgKeysPage.class));
		tabs.add(new UserTab("Access Token", "token", UserAccessTokenPage.class));
		tabs.add(new UserTab("双因素身份验证", "shield", UserTwoFactorAuthenticationPage.class));
		
		add(new Tabbable("userTabs", tabs));
	}
	
	@Override
	protected void onDetach() {
		userModel.detach();
		
		super.onDetach();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new UserCssResourceReference()));
	}
	
	public User getUser() {
		return userModel.getObject();
	}
	
	public static PageParameters paramsOf(User user) {
		PageParameters params = new PageParameters();
		params.add(PARAM_USER, user.getId());
		return params;
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		Fragment fragment = new Fragment(componentId, "topbarTitleFrag", this);
		fragment.add(new BookmarkablePageLink<Void>("users", UserListPage.class));
		fragment.add(new Label("userName", getUser().getDisplayName()));
		return fragment;
	}

}
