package io.onedev.server.plugin.notification.discord;

import io.onedev.server.util.channelnotification.ChannelNotificationSetting;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(name="Discord通知", order=200, description="设置不和谐通知设置。 设置将被子项目继承，并且可以通过使用相同的 webhook url 定义设置来覆盖")
public class DiscordNotificationSetting extends ChannelNotificationSetting {

	private static final long serialVersionUID = 1L;

}
