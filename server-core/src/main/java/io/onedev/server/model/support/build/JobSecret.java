package io.onedev.server.model.support.build;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.util.validation.annotation.SecretName;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Multiline;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
public class JobSecret implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private String value;
	
	private String authorizedBranches;
	
	@Editable(order=100)
	@NotEmpty
	@SecretName
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Editable(order=200)
	@NotEmpty
	@Multiline
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Editable(order=300, placeholder="所有", description=""
			+ "可以选择指定以空格分隔的分支进行授权。\n 只有来自授权分支的构建才能访问此密钥。\n 使用“**”、“*”或“？” 用于<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>。前缀'-' 排除。 留空以授权所有分支机构")
	@Patterns(suggester = "suggestBranches", path=true)
	public String getAuthorizedBranches() {
		return authorizedBranches;
	}

	public void setAuthorizedBranches(String authorizedBranches) {
		this.authorizedBranches = authorizedBranches;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestBranches(String matchWith) {
		Project project = Project.get();
		if (project != null)
			return SuggestionUtils.suggestBranches(project, matchWith);
		else
			return new ArrayList<>();
	}
	
}
