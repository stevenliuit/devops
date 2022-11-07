package io.onedev.server.util.channelnotification;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.RefUpdated;
import io.onedev.server.event.build.BuildEvent;
import io.onedev.server.event.codecomment.CodeCommentEvent;
import io.onedev.server.event.issue.IssueEvent;
import io.onedev.server.event.pullrequest.PullRequestEvent;
import io.onedev.server.search.entity.issue.IssueQueryParseOption;
import io.onedev.server.util.EditContext;
import io.onedev.server.web.editable.annotation.BuildQuery;
import io.onedev.server.web.editable.annotation.CodeCommentQuery;
import io.onedev.server.web.editable.annotation.CommitQuery;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.IssueQuery;
import io.onedev.server.web.editable.annotation.PullRequestQuery;
import io.onedev.server.web.editable.annotation.ShowCondition;

@Editable
public class ChannelNotification implements Serializable {

	private static final long serialVersionUID = 1L;

	private String webhookUrl;
	
	private boolean issues;
	
	private String issueQuery;
	
	private boolean pullRequests;
	
	private String pullRequestQuery;
	
	private boolean builds;
	
	private String buildQuery;
	
	private boolean codePush;
	
	private String commitQuery;

	private boolean codeComments;
	
	private String codeCommentQuery;

	@Editable(order=50, description="指定 webhook url 以发布事件")
	@NotEmpty
	public String getWebhookUrl() {
		return webhookUrl;
	}

	public void setWebhookUrl(String webhookUrl) {
		this.webhookUrl = webhookUrl;
	}

	@Editable(order=100, name="通知问题事件")
	public boolean isIssues() {
		return issues;
	}

	public void setIssues(boolean issues) {
		this.issues = issues;
	}

	@Editable(order=200, name="适用问题", placeholder="所有")
	@IssueQuery(withOrder=false)
	@ShowCondition("isIssuesEnabled")
	public String getIssueQuery() {
		return issueQuery;
	}

	public void setIssueQuery(String issueQuery) {
		this.issueQuery = issueQuery;
	}
	
	@SuppressWarnings("unused")
	private static boolean isIssuesEnabled() {
		return (boolean) EditContext.get().getInputValue("issues");
	}

	@Editable(order=300, name="通知拉取请求事件")
	public boolean isPullRequests() {
		return pullRequests;
	}

	public void setPullRequests(boolean pullRequests) {
		this.pullRequests = pullRequests;
	}

	@Editable(order=400, name="适用的拉取请求", placeholder="所有")
	@PullRequestQuery(withOrder=false)
	@ShowCondition("isPullRequestsEnabled")
	public String getPullRequestQuery() {
		return pullRequestQuery;
	}

	public void setPullRequestQuery(String pullRequestQuery) {
		this.pullRequestQuery = pullRequestQuery;
	}

	@SuppressWarnings("unused")
	private static boolean isPullRequestsEnabled() {
		return (boolean) EditContext.get().getInputValue("pullRequests");
	}
	
	@Editable(order=500, name="通知构建事件")
	public boolean isBuilds() {
		return builds;
	}

	public void setBuilds(boolean builds) {
		this.builds = builds;
	}

	@Editable(order=600, name="适用版本", placeholder="所有")
	@BuildQuery(withOrder=false, withUnfinishedCriteria=true)
	@ShowCondition("isBuildsEnabled")
	public String getBuildQuery() {
		return buildQuery;
	}

	public void setBuildQuery(String buildQuery) {
		this.buildQuery = buildQuery;
	}

	@SuppressWarnings("unused")
	private static boolean isBuildsEnabled() {
		return (boolean) EditContext.get().getInputValue("builds");
	}
	
	@Editable(order=700, name="通知代码推送事件")
	public boolean isCodePush() {
		return codePush;
	}

	public void setCodePush(boolean codePush) {
		this.codePush = codePush;
	}

	@Editable(order=800, name="适用的提交", placeholder="所有")
	@CommitQuery
	@ShowCondition("isCodePushEnabled")
	public String getCommitQuery() {
		return commitQuery;
	}

	public void setCommitQuery(String commitQuery) {
		this.commitQuery = commitQuery;
	}

	@SuppressWarnings("unused")
	private static boolean isCodePushEnabled() {
		return (boolean) EditContext.get().getInputValue("codePush");
	}
	
	@Editable(order=900, name="通知代码注释事件")
	public boolean isCodeComments() {
		return codeComments;
	}

	public void setCodeComments(boolean codeComments) {
		this.codeComments = codeComments;
	}

	@Editable(order=1000, name="适用的代码注释", placeholder="所有")
	@CodeCommentQuery(withOrder=false)
	@ShowCondition("isCodeCommentsEnabled")
	public String getCodeCommentQuery() {
		return codeCommentQuery;
	}

	public void setCodeCommentQuery(String codeCommentQuery) {
		this.codeCommentQuery = codeCommentQuery;
	}

	@SuppressWarnings("unused")
	private static boolean isCodeCommentsEnabled() {
		return (boolean) EditContext.get().getInputValue("codeComments");
	}
	
	public boolean matches(ProjectEvent event) {
		if (event instanceof IssueEvent) {
			if (isIssues()) {
				IssueQueryParseOption option = new IssueQueryParseOption();
				option.withOrder(false);
				return io.onedev.server.search.entity.issue.IssueQuery.parse(event.getProject(), getIssueQuery(), option, true)
						.matches(((IssueEvent)event).getIssue());
			} else {
				return false;
			}
		} else if (event instanceof BuildEvent) {
			if (isBuilds()) {
				return io.onedev.server.search.entity.build.BuildQuery.parse(event.getProject(), getBuildQuery(), false, true)
						.matches(((BuildEvent)event).getBuild());
			} else {
				return false;
			}
		} else if (event instanceof PullRequestEvent) {
			if (isPullRequests()) {
				return io.onedev.server.search.entity.pullrequest.PullRequestQuery.parse(event.getProject(), getPullRequestQuery(), false)
						.matches(((PullRequestEvent)event).getRequest());
			} else {
				return false;
			}
		} else if (event instanceof RefUpdated) {
			if (isCodePush()) {
				return io.onedev.server.search.commit.CommitQuery.parse(event.getProject(), getCommitQuery(), false)
						.matches((RefUpdated)event);
			} else {
				return false;
			}
		} else if (event instanceof CodeCommentEvent) {
			if (isCodeComments()) {
				return io.onedev.server.search.entity.codecomment.CodeCommentQuery.parse(event.getProject(), getCodeCommentQuery(), false)
						.matches(((CodeCommentEvent)event).getComment());
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
}