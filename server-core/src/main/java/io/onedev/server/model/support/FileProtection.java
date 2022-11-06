package io.onedev.server.model.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidatorContext;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.util.reviewrequirement.ReviewRequirement;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.JobChoice;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable
@ClassValidating
public class FileProtection implements Serializable, Validatable {

	private static final long serialVersionUID = 1L;
	
	private String paths;
	
	private String reviewRequirement;
	
	private transient ReviewRequirement parsedReviewRequirement;
	
	private List<String> jobNames = new ArrayList<>();
	
	@Editable(order=100, description="指定要保护的以空格分隔的路径。 使用“**”、“*”或“？” 为了 <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. "
			+ "前缀 '-' 排除")
	@Patterns(suggester = "suggestPaths", path=true)
	@NotEmpty
	public String getPaths() {
		return paths;
	}

	public void setPaths(String paths) {
		this.paths = paths;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestPaths(String matchWith) {
		if (Project.get() != null)
			return SuggestionUtils.suggestBlobs(Project.get(), matchWith);
		else
			return new ArrayList<>();
	}

	@Editable(order=200, name="审稿人", description="如果指定的路径发生更改，请指定所需的审阅者. 请注意，提交更改的用户被视为自动查看更改")
	@io.onedev.server.web.editable.annotation.ReviewRequirement
	public String getReviewRequirement() {
		return reviewRequirement;
	}

	public void setReviewRequirement(String reviewRequirement) {
		this.reviewRequirement = reviewRequirement;
	}
	
	public ReviewRequirement getParsedReviewRequirement() {
		if (parsedReviewRequirement == null)
			parsedReviewRequirement = ReviewRequirement.parse(reviewRequirement, true);
		return parsedReviewRequirement;
	}
	
	public void setParsedReviewRequirement(ReviewRequirement parsedReviewRequirement) {
		this.parsedReviewRequirement = parsedReviewRequirement;
		reviewRequirement = parsedReviewRequirement.toString();
	}
	
	@Editable(order=500, name="所需的构建", placeholder="没有任何", description="（可选）选择所需的构建")
	@JobChoice(tagsMode=true)
	public List<String> getJobNames() {
		return jobNames;
	}

	public void setJobNames(List<String> jobNames) {
		this.jobNames = jobNames;
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		if (getJobNames().isEmpty() && getReviewRequirement() == null) {
			context.disableDefaultConstraintViolation();
			String message = "应指定审阅者或所需的构建";
			context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		} else {
			return true;
		}
	}
	
}
