package io.onedev.server.buildspec.job.trigger;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.buildspec.job.Job;
import io.onedev.server.buildspec.job.SubmitReason;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.ScheduledTimeReaches;
import io.onedev.server.git.GitUtils;
import io.onedev.server.model.PullRequest;
import io.onedev.server.util.validation.annotation.CronExpression;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(order=600, name="Cron时间表")
public class ScheduleTrigger extends JobTrigger {

	private static final long serialVersionUID = 1L;

	private String cronExpression;
	
	@Editable(order=100, description="指定 <a target='_blank' href='http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html#format'>cron调度</a> 到 "
			+ "自动启动作业. <b>Note:</b> 这仅适用于默认分支")
	@CronExpression
	@NotEmpty
	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	@Override
	public SubmitReason triggerMatches(ProjectEvent event, Job job) {
		if (event instanceof ScheduledTimeReaches) {
			return new SubmitReason() {

				@Override
				public String getRefName() {
					return GitUtils.branch2ref(event.getProject().getDefaultBranch());
				}

				@Override
				public PullRequest getPullRequest() {
					return null;
				}

				@Override
				public String getDescription() {
					return "计划时间达到";
				}
				
			};
		} else {
			return null;
		}
	}

	@Override
	public String getTriggerDescription() {
		return "计划时间 " + cronExpression;
	}

}
