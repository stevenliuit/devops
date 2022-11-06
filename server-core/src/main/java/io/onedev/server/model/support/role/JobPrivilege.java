package io.onedev.server.model.support.role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import javax.validation.constraints.NotEmpty;

import edu.emory.mathcs.backport.java.util.Collections;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.util.EditContext;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.editable.annotation.ShowCondition;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
public class JobPrivilege implements Serializable {

	private static final long serialVersionUID = 1L;

	private String jobNames;
	
	private boolean manageJob;
	
	private boolean runJob;
	
	private boolean accessLog;
	
	private String accessibleReports;
	
	@Editable(order=100, description="指定以空格分隔的作业。 使用“*”或“？” 用于通配符匹配. "
			+ "前缀 '-' 排除. <b class='text-danger'>NOTE: </b> 即使此处未指定其他权限，也将在匹配的作业中隐式授予访问构建工件的权限")
	@Patterns(suggester = "suggestJobNames")
	@NotEmpty
	public String getJobNames() {
		return jobNames;
	}

	public void setJobNames(String jobNames) {
		this.jobNames = jobNames;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestJobNames(String matchWith) {
		List<String> jobNames = new ArrayList<>(OneDev.getInstance(BuildManager.class).getJobNames(null));
		Collections.sort(jobNames);
		return SuggestionUtils.suggest(jobNames, matchWith);
	}

	@Editable(order=100, description="作业管理权限，包括删除作业的构建。 它意味着所有其他工作权限")
	public boolean isManageJob() {
		return manageJob;
	}

	public void setManageJob(boolean manageJob) {
		this.manageJob = manageJob;
	}
	
	@SuppressWarnings("unused")
	private static boolean isManageJobDisabled() {
		return !(boolean) EditContext.get().getInputValue("manageJob");
	}

	@Editable(order=200, description="手动运行作业的权限。 它还意味着访问构建日志和所有已发布报告的权限")
	@ShowCondition("isManageJobDisabled")
	public boolean isRunJob() {
		return runJob;
	}

	public void setRunJob(boolean runJob) {
		this.runJob = runJob;
	}

	@SuppressWarnings("unused")
	private static boolean isRunJobDisabled() {
		return !(boolean) EditContext.get().getInputValue("runJob");
	}
	
	@Editable(order=300, name="访问构建日志", description="访问构建日志的权限。 它还意味着访问已发布报告的权限")
	@ShowCondition("isRunJobDisabled")
	public boolean isAccessLog() {
		return accessLog;
	}

	public void setAccessLog(boolean accessLog) {
		this.accessLog = accessLog;
	}

	@SuppressWarnings("unused")
	private static boolean isAccessLogDisabled() {
		return !(boolean) EditContext.get().getInputValue("accessLog");
	}
	
	@Editable(order=400, name="访问构建报告", description="可选择指定以空格分隔的报告. "
			+ "使用“*”或“？” 用于通配符匹配。 前缀 '-' 排除。 留空以匹配所有")
	@ShowCondition("isAccessLogDisabled")
	@Patterns
	@Nullable
	public String getAccessibleReports() {
		return accessibleReports;
	}

	public void setAccessibleReports(String accessibleReports) {
		this.accessibleReports = accessibleReports;
	}

}
