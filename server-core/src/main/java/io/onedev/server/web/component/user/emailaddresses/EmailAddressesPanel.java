package io.onedev.server.web.component.user.emailaddresses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IErrorMessageSource;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.EmailAddressManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.EmailAddress;
import io.onedev.server.model.User;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.EmailAddressVerificationStatusBadge;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.menu.MenuItem;
import io.onedev.server.web.component.menu.MenuLink;
import io.onedev.server.web.page.my.MyPage;
import io.onedev.server.web.util.ConfirmClickModifier;

@SuppressWarnings("serial")
public class EmailAddressesPanel extends GenericPanel<User> {

	private String emailAddressValue;
	
	public EmailAddressesPanel(String id, IModel<User> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (getPage() instanceof MyPage)
			add(new Label("who", "you are "));
		else
			add(new Label("who", "this user is "));
		
		add(new ListView<EmailAddress>("emailAddresses", new AbstractReadOnlyModel<List<EmailAddress>>() {

			@Override
			public List<EmailAddress> getObject() {
				return getUser().getSortedEmailAddresses();
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<EmailAddress> item) {
				EmailAddress address = item.getModelObject();
				item.add(new Label("value", address.getValue()));
				
				item.add(new WebMarkupContainer("primary")
						.setVisible(address.equals(getUser().getPrimaryEmailAddress())));
				item.add(new WebMarkupContainer("git")
						.setVisible(address.equals(getUser().getGitEmailAddress())));
				
				item.add(new EmailAddressVerificationStatusBadge("verificationStatus", item.getModel())); 
				
				if (getUser().isExternalManaged() && address.equals(getUser().getPrimaryEmailAddress())) {
					item.add(new Label("externalManagedNote", 
							"此主电子邮件地址由 " + getUser().getAuthSource()));
				} else {
					item.add(new WebMarkupContainer("externalManagedNote").setVisible(false));
				}
				
				item.add(new MenuLink("operations") {

					@Override
					protected List<MenuItem> getMenuItems(FloatingPanel dropdown) {
						List<MenuItem> menuItems = new ArrayList<>();
						EmailAddress address = item.getModelObject();
						Long addressId = address.getId();
						if (!getUser().isExternalManaged() && !address.equals(getUser().getPrimaryEmailAddress())) {
							menuItems.add(new MenuItem() {

								@Override
								public String getLabel() {
									return "设为主要";
								}

								@Override
								public WebMarkupContainer newLink(String id) {
									return new Link<Void>(id) {

										@Override
										public void onClick() {
											getEmailAddressManager().setAsPrimary(getEmailAddressManager().load(addressId));
										}
										
									};
								}
								
							});
						}
						if (!address.equals(getUser().getGitEmailAddress())) {
							menuItems.add(new MenuItem() {

								@Override
								public String getLabel() {
									return "用于 Git 操作";
								}

								@Override
								public WebMarkupContainer newLink(String id) {
									return new Link<Void>(id) {

										@Override
										public void onClick() {
											getEmailAddressManager().useForGitOperations(getEmailAddressManager().load(addressId));
										}
										
									};
								}
								
							});
						}
						if (!address.isVerified()) {
							menuItems.add(new MenuItem() {

								@Override
								public String getLabel() {
									return "重新发送验证电子邮件";
								}

								@Override
								public WebMarkupContainer newLink(String id) {
									return new AjaxLink<Void>(id) {

										@Override
										public void onClick(AjaxRequestTarget target) {
											if (OneDev.getInstance(SettingManager.class).getMailSetting() != null) {
												getEmailAddressManager().sendVerificationEmail(item.getModelObject());
												Session.get().success("验证邮件已发送，请查收");
											} else {
												target.appendJavaScript(String.format("alert('%s');", 
														"由于系统邮件设置尚未定义，无法发送验证邮件"));
											}
											dropdown.close();
										}
										
									};
								}
								
							});
						}
						if (!(getUser().isExternalManaged() && address.equals(getUser().getPrimaryEmailAddress())) 
								&& getUser().getEmailAddresses().size() > 1) {
							menuItems.add(new MenuItem() {

								@Override
								public String getLabel() {
									return "Delete";
								}

								@Override
								public WebMarkupContainer newLink(String id) {
									Link<Void> link = new Link<Void>(id) {

										@Override
										public void onClick() {
											getEmailAddressManager().delete(getEmailAddressManager().load(addressId));
										}

									};
									link.add(new ConfirmClickModifier("您真的要删除此电子邮件地址吗?"));
									return link;
								}
								
							});
						}
						return menuItems;
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(!getMenuItems(null).isEmpty());
					}
					
				});
			}
			
		});
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				if (getEmailAddressManager().findByValue(emailAddressValue) != null) {
					error("正在使用此电子邮件地址");
				} else {
					EmailAddress address = new EmailAddress();
					address.setValue(emailAddressValue);
					address.setOwner(getUser());
					if (SecurityUtils.isAdministrator())
						address.setVerificationCode(null);
					getEmailAddressManager().save(address);
					emailAddressValue = null;
				}
			}
			
		};
		TextField<String> input = new TextField<String>("emailAddress", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return emailAddressValue;
			}

			@Override
			public void setObject(String object) {
				emailAddressValue = object;
			}
			
		});
		input.setLabel(Model.of("Email address"));
		input.setRequired(true);
		input.add(new IValidator<String>() {

			@Override
			public void validate(IValidatable<String> validatable) {
				String emailAddress = validatable.getValue();
				if (!new EmailValidator().isValid(emailAddress, null)) {
					validatable.error(new IValidationError() {
						
						@Override
						public Serializable getErrorMessage(IErrorMessageSource messageSource) {
							return "格式错误的电子邮件地址";
						}
						
					});
				}
			}
			
		});
		form.add(input);
		add(new FencedFeedbackPanel("feedback", form));
		add(form);
	}
	
	private EmailAddressManager getEmailAddressManager() {
		return OneDev.getInstance(EmailAddressManager.class);
	}

	private User getUser() {
		return getModelObject();
	}

}
