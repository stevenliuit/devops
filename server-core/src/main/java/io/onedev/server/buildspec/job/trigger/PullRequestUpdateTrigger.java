package io.onedev.server.buildspec.job.trigger;

import io.onedev.server.buildspec.job.Job;
import io.onedev.server.buildspec.job.SubmitReason;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.pullrequest.PullRequestMergePreviewCalculated;
import io.onedev.server.model.PullRequest;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(order=300, name="拉取请求打开或更新", description=""
		+ "作业将在目标分支和源分支的合并提交时运行.<br>"
		+ "<b class='text-info'>NOTE:</b> 除非分支保护规则要求, 此触发器将忽略提交 "
		+ "消息包含 <code>[skip ci]</code>, <code>[ci skip]</code>, <code>[no ci]</code>, "
		+ "<code>[skip job]</code>, <code>[job skip]</code>, or <code>[no job]</code>")
public class PullRequestUpdateTrigger extends PullRequestTrigger {

	private static final long serialVersionUID = 1L;

	@Override
	public SubmitReason triggerMatches(ProjectEvent event, Job job) {
		if (event instanceof PullRequestMergePreviewCalculated) {
			PullRequestMergePreviewCalculated mergePreviewCalculated = (PullRequestMergePreviewCalculated) event;
			PullRequest request = mergePreviewCalculated.getRequest();
			if (request.getRequiredJobs().contains(job.getName()) || !SKIP_COMMIT.apply(request.getLatestUpdate().getHeadCommit()))
				return triggerMatches(request);
		}
		return null;
	}

	@Override
	public String getTriggerDescription() {
		return getTriggerDescription("打开/更新");
	}

}
