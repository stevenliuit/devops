package io.onedev.server.model.support.build.actionauthorization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
public abstract class ActionAuthorization implements Serializable {

	private static final long serialVersionUID = 1L;

	private String authorizedBranches;

	@Editable(order=1000, placeholder="所有", description="仅当构建在指定分支上运行时才允许执行操作. 多个分支用空格隔开. "
			+ "使用“**”、“*”或“？” 为了 <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀 '-' 排除。 留空以匹配所有")
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
	
	public abstract String getActionDescription();
	
}
