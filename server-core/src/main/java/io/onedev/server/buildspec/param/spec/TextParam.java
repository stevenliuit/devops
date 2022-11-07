package io.onedev.server.buildspec.param.spec;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import io.onedev.server.model.support.inputspec.textinput.TextInput;
import io.onedev.server.model.support.inputspec.textinput.defaultvalueprovider.DefaultValueProvider;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(order=100, name=ParamSpec.TEXT)
public class TextParam extends ParamSpec {

	private static final long serialVersionUID = 1L;

	private boolean multiline;
	
	private String pattern;
	
	private DefaultValueProvider defaultValueProvider;

	@Editable(order=1050, name="多行")
	public boolean isMultiline() {
		return multiline;
	}

	public void setMultiline(boolean multiline) {
		this.multiline = multiline;
	}

	@Editable(order=1100, description="（可选）指定 <a href='http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html'>正则表达式模式</a> 文本输入的有效值")
	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	@Editable(order=1200, name="默认值", placeholder="无默认值")
	@Valid
	public DefaultValueProvider getDefaultValueProvider() {
		return defaultValueProvider;
	}

	public void setDefaultValueProvider(DefaultValueProvider defaultValueProvider) {
		this.defaultValueProvider = defaultValueProvider;
	}

	@Editable
	@Override
	public boolean isAllowMultiple() {
		return false;
	}

	@Override
	public String getPropertyDef(Map<String, Integer> indexes) {
		return TextInput.getPropertyDef(this, indexes, pattern, multiline, defaultValueProvider);
	}

	@Override
	public Object convertToObject(List<String> strings) {
		return TextInput.convertToObject(strings);
	}

	@Override
	public List<String> convertToStrings(Object value) {
		return TextInput.convertToStrings(value);
	}
	
}
