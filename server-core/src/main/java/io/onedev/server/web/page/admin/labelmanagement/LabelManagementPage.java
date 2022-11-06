package io.onedev.server.web.page.admin.labelmanagement;

import java.util.Comparator;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.LabelManager;
import io.onedev.server.model.LabelSpec;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.page.admin.AdministrationPage;

@SuppressWarnings("serial")
public class LabelManagementPage extends AdministrationPage {

	public LabelManagementPage(PageParameters params) {
		super(params);
	}

	private LabelManager getLabelManager() {
		return OneDev.getInstance(LabelManager.class);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		LabelManagementBean bean = new LabelManagementBean();
		
		var labels = getLabelManager().query();
		labels.sort(Comparator.comparing(LabelSpec::getName));
		bean.getLabels().addAll(labels);
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				getLabelManager().sync(bean.getLabels());
			}
			
		};
		form.add(new FencedFeedbackPanel("feedback", form));
		form.add(BeanContext.edit("editor", bean));
		
		add(form);
	}

	@Override
	protected Component newTopbarTitle(String componentId) {
		return new org.apache.wicket.markup.html.basic.Label(componentId, "标签");
	}

}
