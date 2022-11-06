package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.issue.field.supply.FieldSupply;
import io.onedev.server.util.match.PathMatcher;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedFieldResolution;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedFieldValue;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedFieldValuesResolution;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.FieldNamesProvider;
import io.onedev.server.web.editable.annotation.OmitName;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
public class IssueCreationSetting implements Serializable {

	private static final long serialVersionUID = 1L;

	private String senderEmails;
	
	private String applicableProjects;
	
	private boolean confidential;
	
	private List<FieldSupply> issueFields = new ArrayList<>();

	@Editable(order=100, name="适用发件人", placeholder="任何发件人", 
			description="指定适用于此条目的以空格分隔的发件人电子邮件地址. "
					+ "使用 '*' 或者 '?' 用于通配符匹配. 前缀 '-' 排除. "
					+ "留空以匹配所有发件人")
	@Patterns
	public String getSenderEmails() {
		return senderEmails;
	}

	public void setSenderEmails(String senderEmails) {
		this.senderEmails = senderEmails;
	}

	@Editable(order=150, placeholder="任何项目", description="指定适用于此条目的空格分隔项目. "
			+ "使用 '*' 或者 '?' 用于通配符匹配. 前缀 '-' 排除. "
			+ "匹配所有项目")
	@Patterns(suggester="suggestProjects")
	public String getApplicableProjects() {
		return applicableProjects;
	}

	public void setApplicableProjects(String applicableProjects) {
		this.applicableProjects = applicableProjects;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestProjects(String matchWith) {
		return SuggestionUtils.suggestProjectPaths(matchWith);
	}
	
	@Editable(order=200, description="创建的问题是否应该保密")
	public boolean isConfidential() {
		return confidential;
	}

	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}

	@Editable(order=300)
	@FieldNamesProvider("getFieldNames")
	@OmitName
	@Valid
	public List<FieldSupply> getIssueFields() {
		return issueFields;
	}

	public void setIssueFields(List<FieldSupply> issueFields) {
		this.issueFields = issueFields;
	}
	
	@SuppressWarnings("unused")
	private static Collection<String> getFieldNames() {
		return OneDev.getInstance(SettingManager.class).getIssueSetting().getFieldNames();
	}
	
	public boolean isProjectAuthorized(Project project) {
		return applicableProjects == null 
				|| PatternSet.parse(applicableProjects).matches(new PathMatcher(), project.getPath());
	}
	
	public Set<String> getUndefinedFields() {
		Set<String> undefinedFields = new HashSet<>();
		for (FieldSupply supply: getIssueFields()) 
			undefinedFields.addAll(supply.getUndefinedFields());
		return undefinedFields;
	}

	public Collection<UndefinedFieldValue> getUndefinedFieldValues() {
		Collection<UndefinedFieldValue> undefinedFieldValues = new HashSet<>(); 
		for (FieldSupply supply: getIssueFields()) 
			undefinedFieldValues.addAll(supply.getUndefinedFieldValues());
		return undefinedFieldValues;
	}
	
	public void fixUndefinedFields(Map<String, UndefinedFieldResolution> resolutions) {
		for (Iterator<FieldSupply> it = getIssueFields().iterator(); it.hasNext();) {
			if (!it.next().fixUndefinedFields(resolutions))
				it.remove();
		}
	}

	public void fixUndefinedFieldValues(Map<String, UndefinedFieldValuesResolution> resolutions) {
		for (Iterator<FieldSupply> it = getIssueFields().iterator(); it.hasNext();) {
			if (!it.next().fixUndefinedFieldValues(resolutions))
				it.remove();
		}
	}
	
}