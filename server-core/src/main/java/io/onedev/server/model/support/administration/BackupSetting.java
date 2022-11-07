package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.text.ParseException;

import javax.validation.ConstraintValidatorContext;

import javax.validation.constraints.NotEmpty;
import org.quartz.CronExpression;

import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
@ClassValidating
public class BackupSetting implements Serializable, Validatable {
	
	private static final long serialVersionUID = 1L;
	
	private String schedule;
	
	@Editable(order=100, name="备份计划", description=
		"可以选择指定一个 cron 表达式来安排数据库自动备份。 cron 表达式格式为 <em><seconds>; &lt;分钟&gt; &lt;小时&gt; &lt;月份中的日期&gt; &lt;月&gt; &lt;星期几&gt;</em>。例如，<em>0 0 1 * * ?</em> 表示每天凌晨 1:00。 详细格式参考<a href='http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html#format' target='_blank'>Quartz教程< /a>。备份文件将被放置在系统安装目录下的 <em>db-backup</em> 文件夹中。 如果您不想启用数据库自动备份，请将此属性留空。")
	@NotEmpty
	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		boolean hasErrors = false;
		if (schedule != null) {
			try {
				new CronExpression(schedule);
			} catch (ParseException e) {
				context.buildConstraintViolationWithTemplate(e.getMessage())
						.addPropertyNode("schedule").addConstraintViolation();
				hasErrors = true;
			}
		}
		return !hasErrors;
	}

}
