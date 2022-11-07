package io.onedev.server.web.editable;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.server.util.ReflectionUtils;
import io.onedev.server.web.behavior.InterpolativePatternSetAssistBehavior;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.editable.string.StringPropertyEditor;
import io.onedev.server.web.editable.string.StringPropertyViewer;

@SuppressWarnings("serial")
public class InterpolativePatternsEditSupport implements EditSupport {

	@Override
	public PropertyContext<?> getEditContext(PropertyDescriptor descriptor) {
		Method propertyGetter = descriptor.getPropertyGetter();
		Interpolative interpolative = propertyGetter.getAnnotation(Interpolative.class);
		Patterns patterns = propertyGetter.getAnnotation(Patterns.class);
		
        if (interpolative != null && patterns != null) {
        	if (propertyGetter.getReturnType() == String.class) {
        		return new PropertyContext<String>(descriptor) {

    				@Override
    				public PropertyViewer renderForView(String componentId, IModel<String> model) {
    					return new StringPropertyViewer(componentId, descriptor, model.getObject());
    				}

    				@Override
    				public PropertyEditor<String> renderForEdit(String componentId, IModel<String> model) {
    		        	return new StringPropertyEditor(componentId, descriptor, model).setInputAssist(
    		        			new InterpolativePatternSetAssistBehavior() {

							@SuppressWarnings("unchecked")
							@Override
							protected List<InputSuggestion> suggestPatterns(String matchWith) {
								String suggestionMethod = patterns.suggester();
								if (suggestionMethod.length() != 0) {
									return (List<InputSuggestion>) ReflectionUtils.invokeStaticMethod(
											descriptor.getBeanClass(), suggestionMethod, new Object[] {matchWith});
								} else {
									return Lists.newArrayList();
								}
							}
							
							@SuppressWarnings("unchecked")
							@Override
							protected List<InputSuggestion> suggestVariables(String matchWith) {
								String suggestionMethod = interpolative.variableSuggester();
								if (suggestionMethod.length() != 0) {
									return (List<InputSuggestion>) ReflectionUtils.invokeStaticMethod(
											descriptor.getBeanClass(), suggestionMethod, new Object[] {matchWith});
								} else {
									return Lists.newArrayList();
								}
							}
							
							@Override
							protected List<String> getHints(TerminalExpect terminalExpect) {
								return Lists.newArrayList(
										"需要引用包含空格或以破折号开头的模式",
										patterns.path()? "使用 '**', '*' 或者 '?' 为了 <a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>. 前缀 '-' 排除": "使用“*”或“？” 用于通配符匹配。 前缀 '-' 排除");
							}
							
						});
    				}
        			
        		};
        	} else {
	    		throw new RuntimeException("Annotation 'Interpolative' and 'Patterns' should be applied to property "
	    				+ "of type 'String'");
        	}
        } else {
            return null;
        }
	}

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY - 1;
	}
	
}
