package io.onedev.server.plugin.imports.jiracloud;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.support.administration.GlobalIssueSetting;
import io.onedev.server.model.support.inputspec.InputSpec;
import io.onedev.server.model.support.issue.field.spec.FieldSpec;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class ImportOption implements Serializable {

	private static final long serialVersionUID = 1L;

	private String assigneeIssueField;
	
	private String dueDateIssueField;
	
	private String timeSpentIssueField;
	
	private String timeEstimateIssueField;
	
	private List<IssueStatusMapping> issueStatusMappings = new ArrayList<>();
	
	private List<IssueTypeMapping> issueTypeMappings = new ArrayList<>();
	
	private List<IssuePriorityMapping> issuePriorityMappings = new ArrayList<>();
	
	private static GlobalIssueSetting getIssueSetting() {
		return OneDev.getInstance(SettingManager.class).getIssueSetting();
	}
	
	@Editable(order=350, description="指定一个用户字段来保存受让人信息。<br><b>注意：</b>您可以自定义 系统 问题字段，以防此处没有合适的选项")
	@ChoiceProvider("getAssigneesIssueFieldChoices")
	@NotEmpty
	public String getAssigneeIssueField() {
		return assigneeIssueField;
	}

	public void setAssigneeIssueField(String assigneeIssueField) {
		this.assigneeIssueField = assigneeIssueField;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getAssigneesIssueFieldChoices() {
		List<String> choices = new ArrayList<>();
		for (FieldSpec field: getIssueSetting().getFieldSpecs()) {
			if (field.getType().equals(InputSpec.USER) && field.isAllowMultiple())
				choices.add(field.getName());
		}
		return choices;
	}

	@Editable(order=360, description="可以选择指定一个日期字段来保存到期日期信息。<br><b>注意：</b>如果此处没有适当的选项，您可以自定义 系统 问题字段")
	@ChoiceProvider("getDueDateIssueFieldChoices")
	public String getDueDateIssueField() {
		return dueDateIssueField;
	}

	public void setDueDateIssueField(String dueDateIssueField) {
		this.dueDateIssueField = dueDateIssueField;
	}

	@SuppressWarnings("unused")
	private static List<String> getDueDateIssueFieldChoices() {
		List<String> choices = new ArrayList<>();
		for (FieldSpec field: getIssueSetting().getFieldSpecs()) {
			if (field.getType().equals(InputSpec.DATE))
				choices.add(field.getName());
		}
		return choices;
	}
	
	@Editable(order=370, description="可以选择指定一个工作期间字段来保存所花费的时间信息。<br><b>注意：</b>您可以自定义 系统 问题字段，以防此处没有合适的选项")
	@ChoiceProvider("getWorkingPeriodIssueFieldChoices")
	public String getTimeSpentIssueField() {
		return timeSpentIssueField;
	}

	public void setTimeSpentIssueField(String timeSpentIssueField) {
		this.timeSpentIssueField = timeSpentIssueField;
	}

	@Editable(order=380, description="可以选择指定一个工作周期字段来保存时间估计信息。<br><b>注意：</b>如果此处没有合适的选项，您可以自定义 系统 问题字段")
	@ChoiceProvider("getWorkingPeriodIssueFieldChoices")
	public String getTimeEstimateIssueField() {
		return timeEstimateIssueField;
	}

	public void setTimeEstimateIssueField(String timeEstimateIssueField) {
		this.timeEstimateIssueField = timeEstimateIssueField;
	}

	@SuppressWarnings("unused")
	private static List<String> getWorkingPeriodIssueFieldChoices() {
		List<String> choices = new ArrayList<>();
		for (FieldSpec field: getIssueSetting().getFieldSpecs()) {
			if (field.getType().equals(InputSpec.WORKING_PERIOD))
				choices.add(field.getName());
		}
		return choices;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getVersionIssueFieldChoices() {
		List<String> choices = new ArrayList<>();
		for (FieldSpec field: getIssueSetting().getFieldSpecs()) {
			if (field.getType().equals(InputSpec.TEXT))
				choices.add(field.getName());
		}
		return choices;
	}
	
	@Editable(order=600, description="指定如何将 JIRA 问题状态映射到 OneDev 自定义字段。<br><b>注意：</b>如果此处没有合适的选项，您可以自定义 系统 问题状态")
	public List<IssueStatusMapping> getIssueStatusMappings() {
		return issueStatusMappings;
	}

	public void setIssueStatusMappings(List<IssueStatusMapping> issueStatusMappings) {
		this.issueStatusMappings = issueStatusMappings;
	}

	@Editable(order=700, description="指定如何将 JIRA 问题类型映射到 OneDev 自定义字段。<br><b>注意：</b>如果此处没有合适的选项，您可以自定义 系统 问题字段")
	public List<IssueTypeMapping> getIssueTypeMappings() {
		return issueTypeMappings;
	}

	public void setIssueTypeMappings(List<IssueTypeMapping> issueTypeMappings) {
		this.issueTypeMappings = issueTypeMappings;
	}

	@Editable(order=800, description="指定如何将 JIRA 问题优先级映射到 OneDev 自定义字段。<br><b>注意：</b>如果此处没有合适的选项，您可以自定义 系统 问题字段")
	public List<IssuePriorityMapping> getIssuePriorityMappings() {
		return issuePriorityMappings;
	}

	public void setIssuePriorityMappings(List<IssuePriorityMapping> issuePriorityMappings) {
		this.issuePriorityMappings = issuePriorityMappings;
	}

}
