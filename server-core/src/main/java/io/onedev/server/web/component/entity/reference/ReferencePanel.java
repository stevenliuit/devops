package io.onedev.server.web.component.entity.reference;

import io.onedev.server.entityreference.Referenceable;
import io.onedev.server.web.component.link.copytoclipboard.CopyToClipboardLink;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public abstract class ReferencePanel extends Panel {
	
	public ReferencePanel(String id) {
		super(id);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new WebMarkupContainer("referenceHelp") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("title", "参考这个 " + getReferenceable().getType() 
						+ " 通过以下字符串在降价或提交消息中。 如果从当前项目中引用，项目名称可以省略");
			}
			
		});
		
		String reference = Referenceable.asReference(getReferenceable());
		
		add(new Label("reference", reference));
		add(new CopyToClipboardLink("copy", Model.of(reference)));
	}
	
	protected abstract Referenceable getReferenceable();
}
