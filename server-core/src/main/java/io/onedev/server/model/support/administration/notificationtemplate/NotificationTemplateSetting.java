package io.onedev.server.model.support.administration.notificationtemplate;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Size;

import com.google.common.io.Resources;

import io.onedev.server.util.validation.annotation.Code;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.OmitName;

@Editable
public class NotificationTemplateSetting implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final List<String> DEFAULT_TEMPLATE;
	
	public static final String PROP_ISSUE_NOTIFICATION_TEMPLATE = "issueNotificationTemplate";
	
	public static final String PROP_PULL_REQUEST_NOTIFICATION_TEMPLATE = "pullRequestNotificationTemplate";
	
	public static final String COMMON_HELP = "通知模板是 "
			+ "<a href='https://docs.groovy-lang.org/latest/html/api/groovy/text/SimpleTemplateEngine.html' target='_blank'>Groovy 简单模板</a>. "
			+ "评估此模板时，以下变量将可用:"
			+ "<ul class='mb-0'>"
			+ "<li><code>event:</code> <a href='https://code.onedev.io/projects/160/blob/main/server-core/src/main/java/io/onedev/server/event/Event.java' target='_blank'>事件对象</a> 触发通知"
			+ "<li><code>eventSummary:</code> 表示事件摘要的字符串"
			+ "<li><code>eventBody:</code> 表示事件主体的字符串。 或许 <code>null</code>"
			+ "<li><code>eventUrl:</code> 表示事件详细信息 url 的字符串"
			+ "<li><code>replyable:</code> 一个boolean值，指示是否可以通过回复电子邮件直接创建主题评论"
			+ "<li><code>unsubscribable:</code> 一个 <a href='https://code.onedev.io/projects/160/blob/main/server-core/src/main/java/io/onedev/server/notification/Unsubscribable.java' target='_blank'>object</a> 持有退订信息. "
			+ "		A <code>null</code> value 表示不能取消订阅通知";
	
	static {
		URL url = Resources.getResource(NotificationTemplateSetting.class, "default-notification-template.html");
		try {
			DEFAULT_TEMPLATE = Resources.readLines(url, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private List<String> issueNotificationTemplate = DEFAULT_TEMPLATE;

	private List<String> pullRequestNotificationTemplate = DEFAULT_TEMPLATE;
	
	@Editable(order=200)
	@Code(language=Code.HTML_TEMPLATE)
	@OmitName
	@Size(min=1, message="不能为空")
	public List<String> getIssueNotificationTemplate() {
		return issueNotificationTemplate;
	}

	public void setIssueNotificationTemplate(List<String> issueNotificationTemplate) {
		this.issueNotificationTemplate = issueNotificationTemplate;
	}

	@Editable(order=300)
	@Code(language=Code.HTML_TEMPLATE)
	@OmitName
	@Size(min=1, message="不能为空")
	public List<String> getPullRequestNotificationTemplate() {
		return pullRequestNotificationTemplate;
	}

	public void setPullRequestNotificationTemplate(List<String> pullRequestNotificationTemplate) {
		this.pullRequestNotificationTemplate = pullRequestNotificationTemplate;
	}
	
	public static String getTemplateHelp(Map<String, String> variableHelp) {
		StringBuilder builder = new StringBuilder(COMMON_HELP);
		for (Map.Entry<String, String> entry: variableHelp.entrySet())
			builder.append("<li><code>" + entry.getKey() + ":</code> " + entry.getValue());
		return builder.toString();
	}
	
}
