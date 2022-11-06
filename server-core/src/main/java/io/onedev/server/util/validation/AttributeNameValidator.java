package io.onedev.server.util.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.onedev.server.model.Agent;
import io.onedev.server.util.validation.annotation.AttributeName;

public class AttributeNameValidator implements ConstraintValidator<AttributeName, String> {

	public static final Pattern PATTERN = Pattern.compile("\\w([\\w-\\.\\s]*\\w)?");

	private String message;
	
	@Override
	public void initialize(AttributeName constaintAnnotation) {
		message = constaintAnnotation.message();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext constraintContext) {
		if (value == null) 
			return true;

		String message = this.message;
		if (!PATTERN.matcher(value).matches()) {
			if (message.length() == 0) {
				message = "应该以字母数字或下划线开头和结尾. "
						+ "中间只允许使用字母数字、下划线、破折号和点。";
			}
			constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		} else if (Agent.ALL_FIELDS.contains(value)) {
			constraintContext.disableDefaultConstraintViolation();
			if (message.length() == 0)
				message = "'" + value + "' 被预定了";
			constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		} else {
			return true;
		}
	}
	
}
