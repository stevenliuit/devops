package io.onedev.server.web.page.admin.buildsetting.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;

import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.AgentManager;
import io.onedev.server.model.Agent;
import io.onedev.server.search.entity.EntitySort;
import io.onedev.server.search.entity.agent.AgentQuery;
import io.onedev.server.search.entity.agent.NameCriteria;
import io.onedev.server.search.entity.agent.OsArchCriteria;
import io.onedev.server.search.entity.agent.OsCriteria;
import io.onedev.server.search.entity.agent.OsVersionCriteria;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.util.criteria.OrCriteria;
import io.onedev.server.web.WebConstants;
import io.onedev.server.web.behavior.AgentQueryBehavior;
import io.onedev.server.web.component.AgentStatusBadge;
import io.onedev.server.web.component.datatable.DefaultDataTable;
import io.onedev.server.web.component.datatable.selectioncolumn.SelectionColumn;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.link.DropdownLink;
import io.onedev.server.web.component.menu.MenuItem;
import io.onedev.server.web.component.menu.MenuLink;
import io.onedev.server.web.component.modal.confirm.ConfirmModalPanel;
import io.onedev.server.web.component.orderedit.OrderEditPanel;
import io.onedev.server.web.component.savedquery.SavedQueriesClosed;
import io.onedev.server.web.component.savedquery.SavedQueriesOpened;
import io.onedev.server.web.util.LoadableDetachableDataProvider;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
class AgentListPanel extends Panel {
	
	private final IModel<String> queryStringModel;
	
	private final IModel<AgentQuery> queryModel = new LoadableDetachableModel<AgentQuery>() {

		@Override
		protected AgentQuery load() {
			String queryString = queryStringModel.getObject();
			try {
				return AgentQuery.parse(queryString, false);
			} catch (ExplicitException e) {
				error(e.getMessage());
				return null;
			} catch (Exception e) {
				info("Performing fuzzy query");
				List<Criteria<Agent>> criterias = new ArrayList<>();
				criterias.add(new NameCriteria("*" + queryString + "*"));
				criterias.add(new OsCriteria("*" + queryString + "*"));
				criterias.add(new OsVersionCriteria("*" + queryString + "*"));
				criterias.add(new OsArchCriteria("*" + queryString + "*"));
				return new AgentQuery(new OrCriteria<Agent>(criterias));
			}
		}
		
	};
	
	private DataTable<Agent, Void> agentsTable;
	
	private SelectionColumn<Agent, Void> selectionColumn;
	
	private SortableDataProvider<Agent, Void> dataProvider;	
	
	private WebMarkupContainer body;
	
	private Component saveQueryLink;	
	
	private TextField<String> queryInput;
	
	private boolean querySubmitted = true;
	
	public AgentListPanel(String id, IModel<String> queryModel) {
		super(id);
		this.queryStringModel = queryModel;
	}
	
	private AgentManager getAgentManager() {
		return OneDev.getInstance(AgentManager.class);
	}
	
	@Override
	protected void onDetach() {
		queryStringModel.detach();
		queryModel.detach();
		super.onDetach();
	}
	
	@Nullable
	protected PagingHistorySupport getPagingHistorySupport() {
		return null;
	}
	
	@Nullable
	protected QuerySaveSupport getQuerySaveSupport() {
		return null;
	}

	private void doQuery(AjaxRequestTarget target) {
		agentsTable.setCurrentPage(0);
		target.add(body);
		selectionColumn.getSelections().clear();
		querySubmitted = true;
		if (SecurityUtils.getUser() != null && getQuerySaveSupport() != null)
			target.add(saveQueryLink);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new AjaxLink<Void>("showSavedQueries") {

			@Override
			public void onEvent(IEvent<?> event) {
				super.onEvent(event);
				if (event.getPayload() instanceof SavedQueriesClosed) 
					((SavedQueriesClosed) event.getPayload()).getHandler().add(this);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getQuerySaveSupport() != null && !getQuerySaveSupport().isSavedQueriesVisible());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				send(getPage(), Broadcast.BREADTH, new SavedQueriesOpened(target));
				target.add(this);
			}
			
		}.setOutputMarkupPlaceholderTag(true));

		add(saveQueryLink = new AjaxLink<Void>("saveQuery") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setEnabled(querySubmitted && queryModel.getObject() != null);
				setVisible(getQuerySaveSupport() != null);
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				configure();
				if (!isEnabled()) 
					tag.append("class", "disabled", " ");
				if (!querySubmitted)
					tag.put("title", "查询未提交");
				else if (queryModel.getObject() == null)
					tag.put("title", "无法保存格式错误的查询");
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				getQuerySaveSupport().onSaveQuery(target, queryModel.getObject().toString());
			}		
			
		}.setOutputMarkupPlaceholderTag(true));
		
		add(new DropdownLink("orderBy") {

			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				List<String> orderFields = new ArrayList<>(Agent.ORDER_FIELDS.keySet());
				return new OrderEditPanel<Agent>(id, orderFields, new IModel<List<EntitySort>> () {

					@Override
					public void detach() {
					}

					@Override
					public List<EntitySort> getObject() {
						AgentQuery query = queryModel.getObject();
						AgentListPanel.this.getFeedbackMessages().clear();
						if (query != null) 
							return query.getSorts();
						else
							return new ArrayList<>();
					}

					@Override
					public void setObject(List<EntitySort> object) {
						AgentQuery query = queryModel.getObject();
						AgentListPanel.this.getFeedbackMessages().clear();
						if (query == null)
							query = new AgentQuery();
						query.getSorts().clear();
						query.getSorts().addAll(object);
						queryModel.setObject(query);
						queryStringModel.setObject(query.toString());
						AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class); 
						target.add(queryInput);
						doQuery(target);
					}
					
				});
			}
			
		});
		
		add(new DropdownLink("tokens") {

			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				return new TokenListPanel(id);
			}
			
		});
		
		add(new MenuLink("operations") {

			@Override
			protected List<MenuItem> getMenuItems(FloatingPanel dropdown) {
				List<MenuItem> menuItems = new ArrayList<>();
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "暂停选定的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								Collection<Agent> agents = new ArrayList<>();
								for (IModel<Agent> each: selectionColumn.getSelections())
									agents.add(each.getObject());
								OneDev.getInstance(AgentManager.class).pause(agents);
								target.add(body);
								selectionColumn.getSelections().clear();
								Session.get().success("暂停选定的Agents");
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(!selectionColumn.getSelections().isEmpty());
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "请选择要暂停的Agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "恢复选定的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								Collection<Agent> agents = new ArrayList<>();
								for (IModel<Agent> each: selectionColumn.getSelections())
									agents.add(each.getObject());
								OneDev.getInstance(AgentManager.class).resume(agents);
								target.add(body);
								selectionColumn.getSelections().clear();
								Session.get().success("恢复选定的agents");
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(!selectionColumn.getSelections().isEmpty());
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "请选择要恢复的agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "重新启动选定Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (IModel<Agent> each: selectionColumn.getSelections())
											agents.add(each.getObject());
										OneDev.getInstance(AgentManager.class).restart(agents);
										target.add(body);
										selectionColumn.getSelections().clear();
										Session.get().success("向选定agents发出的重新启动命令");
									}
									
									@Override
									protected String getConfirmMessage() {
										return "在下面键入 <code>yes</code> 以重新启动选定的agents";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(!selectionColumn.getSelections().isEmpty());
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "请选择要重新启动的agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "删除选定的离线Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (IModel<Agent> each: selectionColumn.getSelections()) {
											Agent agent = each.getObject();
											if (!agent.isOnline())
												agents.add(agent);
										}
										OneDev.getInstance(AgentManager.class).delete(agents);
										selectionColumn.getSelections().clear();
										target.add(body);
									}
									
									@Override
									protected String getConfirmMessage() {
										return "删除了选定的离线agents。 请注意，这些agents使用的令牌不会被删除，如果上线，它们会重新出现在这里。 在下方输入 <code>yes</code> 进行确认";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(!selectionColumn.getSelections().isEmpty());
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "请选择要删除的离线agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "取消授权选定的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (IModel<Agent> each: selectionColumn.getSelections()) {
											Agent agent = each.getObject();
											if (!agent.isOnline())
												agents.add(agent);
										}
										OneDev.getInstance(AgentManager.class).unauthorize(agents);
										selectionColumn.getSelections().clear();
										target.add(body);
									}
									
									@Override
									protected String getConfirmMessage() {
										return "删除选定的agents以及它们使用的令牌。 请注意，与这些agents共享 okens 的其他agents也将被删除。 在下方输入 <code>yes</code> 进行确认";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(!selectionColumn.getSelections().isEmpty());
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "请选择要删除的离线agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "暂停所有查询的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (Iterator<Agent> it = (Iterator<Agent>) dataProvider.iterator(0, agentsTable.getItemCount()); it.hasNext();) {
											agents.add(it.next());
										}
										OneDev.getInstance(AgentManager.class).pause(agents);
										selectionColumn.getSelections().clear();
										target.add(body);
										Session.get().success("暂停所有查询的agents");
									}
									
									@Override
									protected String getConfirmMessage() {
										return "在下面键入 <code>yes</code> 以暂停所有查询的agents";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(agentsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有agents可以暂停");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "恢复所有查询的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (Iterator<Agent> it = (Iterator<Agent>) dataProvider.iterator(0, agentsTable.getItemCount()); it.hasNext();) {
											agents.add(it.next());
										}
										OneDev.getInstance(AgentManager.class).resume(agents);
										target.add(body);
										selectionColumn.getSelections().clear();
										Session.get().success("恢复所有查询的代理agents");
									}
									
									@Override
									protected String getConfirmMessage() {
										return "在下面输入 <code>yes</code> 以恢复所有查询的agents";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(agentsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有要恢复的agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "重新启动所有查询的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (Iterator<Agent> it = (Iterator<Agent>) dataProvider.iterator(0, agentsTable.getItemCount()); it.hasNext();) {
											agents.add(it.next());
										}
										OneDev.getInstance(AgentManager.class).restart(agents);
										target.add(body);
										selectionColumn.getSelections().clear();
										Session.get().success("向所有查询的agents发出重新启动命令");
									}
									
									@Override
									protected String getConfirmMessage() {
										return "在下面输入 <code>yes</code> 以重新启动所有查询的agents";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(agentsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有要重新启动的agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "删除所有查询的离线Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (Iterator<Agent> it = (Iterator<Agent>) dataProvider.iterator(0, agentsTable.getItemCount()); it.hasNext();) {
											Agent agent = it.next();
											if (!agent.isOnline())
												agents.add(agent);
										}
										OneDev.getInstance(AgentManager.class).delete(agents);
										target.add(body);
										selectionColumn.getSelections().clear();
									}
									
									@Override
									protected String getConfirmMessage() {
										return "删除所有查询的离线agents。 请注意，这不会删除这些agents使用的令牌，如果上线，它们将重新出现在此处。 在下方输入 <code>yes</code> 进行确认";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(agentsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有要删除的离线agents");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "取消授权所有查询的Agents";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								new ConfirmModalPanel(target) {
									
									@Override
									protected void onConfirm(AjaxRequestTarget target) {
										Collection<Agent> agents = new ArrayList<>();
										for (Iterator<Agent> it = (Iterator<Agent>) dataProvider.iterator(0, agentsTable.getItemCount()); it.hasNext();) {
											agents.add(it.next());
										}
										OneDev.getInstance(AgentManager.class).unauthorize(agents);
										target.add(body);
										selectionColumn.getSelections().clear();
									}
									
									@Override
									protected String getConfirmMessage() {
										return "删除所有查询的agents以及它们使用的令牌。 请注意，与这些agents共享令牌的其他agents也将被删除。 在下方输入 <code>yes</code> 进行确认";
									}
									
									@Override
									protected String getConfirmInput() {
										return "yes";
									}
									
								};
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(agentsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有agents可以取消授权");
								}
							}
							
						};
					}
					
				});
				
				return menuItems;
			}
			
		});
		
		queryInput = new TextField<String>("input", queryStringModel);
		queryInput.setOutputMarkupId(true);
		queryInput.add(new AgentQueryBehavior(false) {

			@Override
			protected void onInput(AjaxRequestTarget target, String inputContent) {
				AgentListPanel.this.getFeedbackMessages().clear();
				querySubmitted = StringUtils.trimToEmpty(queryStringModel.getObject())
						.equals(StringUtils.trimToEmpty(inputContent));
				target.add(saveQueryLink);
			}
			
			@Override
			protected List<String> getHints(TerminalExpect terminalExpect) {
				List<String> hints = super.getHints(terminalExpect);
				hints.add("Free input for fuzzy query on name/os");
				return hints;
			}
			
		});
		
		queryInput.add(new AjaxFormComponentUpdatingBehavior("clear") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				doQuery(target);
			}
			
		});
		
		Form<?> queryForm = new Form<Void>("query");
		queryForm.add(queryInput);
		queryForm.add(new AjaxButton("submit") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				doQuery(target);
			}
			
		});
		add(queryForm);
		
		add(new DropdownLink("addAgent") {

			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				return new AddAgentPanel(id);
			}

		});
		
		dataProvider = new LoadableDetachableDataProvider<Agent, Void>() {

			@Override
			public Iterator<? extends Agent> iterator(long first, long count) {
				try {
					AgentQuery query = queryModel.getObject();
					if (query != null)
						return getAgentManager().query(query, (int)first, (int)count).iterator();
				} catch (ExplicitException e) {
					error(e.getMessage());
				}
				return new ArrayList<Agent>().iterator();
			}

			@Override
			public long calcSize() {
				try {
					AgentQuery query = queryModel.getObject();
					if (query != null) 
						return getAgentManager().count(query.getCriteria());
				} catch (ExplicitException e) {
					error(e.getMessage());
				}
				return 0;
			}

			@Override
			public IModel<Agent> model(Agent object) {
				Long agentId = object.getId();
				return new LoadableDetachableModel<Agent>() {

					@Override
					protected Agent load() {
						return getAgentManager().load(agentId);
					}
					
				};
			}
			
		};
		
		body = new WebMarkupContainer("body");
		add(body.setOutputMarkupId(true));
		
		body.add(new FencedFeedbackPanel("feedback", this));
		
		List<IColumn<Agent, Void>> columns = new ArrayList<>();
		
		columns.add(selectionColumn = new SelectionColumn<Agent, Void>());
		
		columns.add(new AbstractColumn<Agent, Void>(Model.of("Name")) {

			@Override
			public void populateItem(Item<ICellPopulator<Agent>> cellItem, String componentId, IModel<Agent> rowModel) {
				Fragment fragment = new Fragment(componentId, "agentLinkFrag", AgentListPanel.this);
				Agent agent = rowModel.getObject();
				Link<Void> link = new BookmarkablePageLink<Void>("link", 
						AgentOverviewPage.class, AgentOverviewPage.paramsOf(agent));
				link.add(new AgentIcon("icon", rowModel));
				link.add(new Label("label", agent.getName()));
				fragment.add(link);
				cellItem.add(fragment);
			}
			
		});
		
		columns.add(new AbstractColumn<Agent, Void>(Model.of("IP Address")) {

			@Override
			public void populateItem(Item<ICellPopulator<Agent>> cellItem, String componentId, IModel<Agent> rowModel) {
				Agent agent = rowModel.getObject();
				cellItem.add(new Label(componentId, agent.getIpAddress()));
			}
			
			@Override
			public String getCssClass() {
				return "d-none d-xl-table-cell";
			}
			
		});
		
		columns.add(new AbstractColumn<Agent, Void>(Model.of("CPU")) {

			@Override
			public void populateItem(Item<ICellPopulator<Agent>> cellItem, String componentId, IModel<Agent> rowModel) {
				Agent agent = rowModel.getObject();
				cellItem.add(new Label(componentId, agent.getCpu()));
			}

			@Override
			public String getCssClass() {
				return "d-none d-xl-table-cell";
			}

			@Override
			public Component getHeader(String componentId) {
				return new Fragment(componentId, "cpuHeaderFrag", AgentListPanel.this);
			}
			
		});
		
		columns.add(new AbstractColumn<Agent, Void>(Model.of("Memory")) {

			@Override
			public void populateItem(Item<ICellPopulator<Agent>> cellItem, String componentId, IModel<Agent> rowModel) {
				Agent agent = rowModel.getObject();
				cellItem.add(new Label(componentId, agent.getMemory()));
			}
			
			@Override
			public String getCssClass() {
				return "d-none d-xl-table-cell";
			}
			
			@Override
			public Component getHeader(String componentId) {
				return new Fragment(componentId, "memoryHeaderFrag", AgentListPanel.this);
			}
			
		});
		
		columns.add(new AbstractColumn<Agent, Void>(Model.of("Temporal")) {

			@Override
			public void populateItem(Item<ICellPopulator<Agent>> cellItem, String componentId, IModel<Agent> rowModel) {
				cellItem.add(new Label(componentId, rowModel.getObject().isTemporal()));
			}
			
			@Override
			public Component getHeader(String componentId) {
				return new Fragment(componentId, "temporalHeaderFrag", AgentListPanel.this);
			}
			
		});
		
		columns.add(new AbstractColumn<Agent, Void>(Model.of("Status")) {

			@Override
			public void populateItem(Item<ICellPopulator<Agent>> cellItem, String componentId, IModel<Agent> rowModel) {
				cellItem.add(new AgentStatusBadge(componentId, rowModel));
			}
			
		});
		
		body.add(agentsTable = new DefaultDataTable<Agent, Void>("agents", columns, dataProvider, 
				WebConstants.PAGE_SIZE, getPagingHistorySupport()));
		
		setOutputMarkupId(true);
	}

}
