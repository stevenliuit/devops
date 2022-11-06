package io.onedev.server.web.page.my.password;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.model.User;
import io.onedev.server.web.component.user.passwordedit.PasswordEditPanel;
import io.onedev.server.web.page.my.MyPage;

@SuppressWarnings("serial")
public class MyPasswordPage extends MyPage {
	
	public MyPasswordPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (getLoginUser().getPassword().equals(User.EXTERNAL_MANAGED)) {
			String message;
			if (getLoginUser().getSsoConnector() != null) {
				message = "您目前通过 SSO 提供商进行身份验证 '" 
						+ getLoginUser().getSsoConnector() 
						+ "', 请在SSO提供商更改密码";
			} else {
				message = "您当前已通过外部系统进行身份验证, "
						+ "请在SSO提供商更改密码";
			}
			add(new Label("content", message).add(AttributeAppender.append("class", "alert alert-light-warning alert-notice mb-0")));
		} else {
			add(new PasswordEditPanel("content", new AbstractReadOnlyModel<User>() {

				@Override
				public User getObject() {
					return getLoginUser();
				}
				
			}));
		}
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		return new Label(componentId, "修改我的密码");
	}

}
