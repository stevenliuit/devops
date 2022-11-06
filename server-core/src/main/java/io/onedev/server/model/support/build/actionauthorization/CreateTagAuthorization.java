package io.onedev.server.model.support.build.actionauthorization;

import java.util.ArrayList;
import java.util.List;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable(order=20, name="Create tag")
public class CreateTagAuthorization extends ActionAuthorization {

	private static final long serialVersionUID = 1L;

	private String tagNames;

	@Editable(order=100, placeholder="所有", description="指定以空格分隔的标签名称. "
			+ "使用 '**', '*' 或者 '?' 为了 <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀 '-' 排除。 留空以匹配所有")
	@Patterns(suggester = "suggestTags", path=true)
	public String getTagNames() {
		return tagNames;
	}

	public void setTagNames(String tagNames) {
		this.tagNames = tagNames;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestTags(String matchWith) {
		if (Project.get() != null)
			return SuggestionUtils.suggestTags(Project.get(), matchWith);
		else
			return new ArrayList<>();
	}

	@Override
	public String getActionDescription() {
		if (tagNames != null)
			return "创建名称匹配的标签 '" + tagNames + "'";
		else
			return "创建标签";
	}
	
}
