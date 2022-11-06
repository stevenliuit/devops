package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import edu.emory.mathcs.backport.java.util.Collections;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;

@Editable
public class ProjectDesignation implements Serializable {

	private static final long serialVersionUID = 1L;

	private String senderEmails;
	
	private String project;

	@Editable(order=100, name="适用发件人", placeholder="任何发件人", description=""
			+ "指定适用于此条目的以空格分隔的发件人电子邮件地址. "
			+ "使用 '*' 或者 '?' 用于通配符匹配. 前缀 '-' 排除. "
			+ "留空以匹配所有发件人")
	@Patterns
	public String getSenderEmails() {
		return senderEmails;
	}

	public void setSenderEmails(String senderEmails) {
		this.senderEmails = senderEmails;
	}
	
	@Editable(order=200)
	@ChoiceProvider("getProjectChoices")
	@NotEmpty
	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	@SuppressWarnings("unused")
	private static List<String> getProjectChoices() {
		List<String> projectPaths = OneDev.getInstance(ProjectManager.class)
				.query().stream().map(it->it.getPath()).collect(Collectors.toList());
		Collections.sort(projectPaths);
		return projectPaths;
	}
		
}