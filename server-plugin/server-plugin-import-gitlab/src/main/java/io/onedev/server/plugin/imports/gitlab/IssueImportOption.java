package io.onedev.server.plugin.imports.gitlab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.support.administration.GlobalIssueSetting;
import io.onedev.server.model.support.inputspec.InputSpec;
import io.onedev.server.model.support.issue.field.spec.FieldSpec;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class IssueImportOption implements Serializable {

	private static final long serialVersionUID = 1L;

	private String closedIssueState;
	
	private String assigneesIssueField;
	
	private String dueDateIssueField;
	
	private String estimatedTimeIssueField;
	
	private String spentTimeIssueField;
	
	private List<IssueLabelMapping> issueLabelMappings = new ArrayList<>();
	
	@Editable(order=300, description="指定用于已关闭 GitLab 问题的问题状态。<br><b>注意：</b> 如果此处没有合适的选项，您可以自定义 系统 问题状态")
	@ChoiceProvider("getCloseStateChoices")
	@NotEmpty
	public String getClosedIssueState() {
		return closedIssueState;
	}

	public void setClosedIssueState(String closedIssueState) {
		this.closedIssueState = closedIssueState;
	}

	private static GlobalIssueSetting getIssueSetting() {
		return OneDev.getInstance(SettingManager.class).getIssueSetting();
	}
	
	@SuppressWarnings("unused")
	private static List<String> getCloseStateChoices() {
		List<String> choices = getIssueSetting().getStateSpecs().stream()
				.map(it->it.getName()).collect(Collectors.toList());
		choices.remove(0);
		return choices;
	}
	
	@Editable(order=350, description="指定一个多值用户字段来保存受让人信息。<br><b>注意：</b>如果此处没有合适的选项，您可以自定义 系统 问题字段")
	@ChoiceProvider("getAssigneesIssueFieldChoices")
	@NotEmpty
	public String getAssigneesIssueField() {
		return assigneesIssueField;
	}

	public void setAssigneesIssueField(String assigneesIssueField) {
		this.assigneesIssueField = assigneesIssueField;
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
	
	@Editable(order=370, description="可以选择指定工作期间字段以保存估计的时间信息。<br><b>注意：</b>如果此处没有合适的选项，您可以自定义 系统 问题字段")
	@ChoiceProvider("getWorkingPeriodIssueFieldChoices")
	public String getEstimatedTimeIssueField() {
		return estimatedTimeIssueField;
	}

	public void setEstimatedTimeIssueField(String estimatedTimeIssueField) {
		this.estimatedTimeIssueField = estimatedTimeIssueField;
	}

	@Editable(order=380, description="可以选择指定一个工作时间字段来保存花费的时间信息。<br><b>注意：</b>如果此处没有适当的选项，您可以自定义 系统 问题字段")
	@ChoiceProvider("getWorkingPeriodIssueFieldChoices")
	public String getSpentTimeIssueField() {
		return spentTimeIssueField;
	}

	public void setSpentTimeIssueField(String spentTimeIssueField) {
		this.spentTimeIssueField = spentTimeIssueField;
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
	
	@Editable(order=400, description="指定如何将 GitLab 问题标签映射到 OneDev 自定义字段。<br><b>注意：</b> 如果此处没有合适的选项，您可以自定义 系统 问题字段")
	public List<IssueLabelMapping> getIssueLabelMappings() {
		return issueLabelMappings;
	}

	public void setIssueLabelMappings(List<IssueLabelMapping> issueLabelMappings) {
		this.issueLabelMappings = issueLabelMappings;
	}

}
