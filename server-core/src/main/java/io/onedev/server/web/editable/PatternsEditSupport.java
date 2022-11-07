package io.onedev.server.web.editable;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.server.util.ReflectionUtils;
import io.onedev.server.web.behavior.PatternSetAssistBehavior;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.editable.string.StringPropertyEditor;
import io.onedev.server.web.editable.string.StringPropertyViewer;

@SuppressWarnings("serial")
public class PatternsEditSupport implements EditSupport {

	@Override
	public PropertyContext<?> getEditContext(PropertyDescriptor descriptor) {
		Method propertyGetter = descriptor.getPropertyGetter();
		Patterns patterns = propertyGetter.getAnnotation(Patterns.class);
        if (patterns != null) {
        	if (propertyGetter.getReturnType() == String.class) {
        		return new PropertyContext<String>(descriptor) {

    				@Override
    				public PropertyViewer renderForView(String componentId, IModel<String> model) {
    					return new StringPropertyViewer(componentId, descriptor, model.getObject());
    				}

    				@Override
    				public PropertyEditor<String> renderForEdit(String componentId, IModel<String> model) {
    		        	return new StringPropertyEditor(componentId, descriptor, model).setInputAssist(
    		        			new PatternSetAssistBehavior() {

							@SuppressWarnings("unchecked")
							@Override
							protected List<InputSuggestion> suggest(String matchWith) {
								String suggestionMethod = patterns.suggester();
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
										patterns.path()? "使用“**”、“*”或“？” 用于<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>。 前缀 '-' 排除": "使用 '*' 或 '?' 用于通配符匹配。 前缀 '-' 排除"
										);
							}
							
						});
    				}
        			
        		};
        	} else {
	    		throw new RuntimeException("注释“Patterns”应应用于“String”类型的属性");
        	}
        } else {
            return null;
        }
	}

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}
	
}
