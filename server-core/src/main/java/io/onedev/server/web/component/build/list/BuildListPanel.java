package io.onedev.server.web.component.build.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
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
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.FileMode;

import com.google.common.collect.Sets;

import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.job.JobManager;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.entitymanager.BuildParamManager;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.git.BlobIdent;
import io.onedev.server.model.Build;
import io.onedev.server.model.Build.Status;
import io.onedev.server.model.BuildLabel;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.administration.GlobalBuildSetting;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.search.entity.EntitySort;
import io.onedev.server.search.entity.build.BuildQuery;
import io.onedev.server.search.entity.build.BuildQueryLexer;
import io.onedev.server.search.entity.build.JobCriteria;
import io.onedev.server.search.entity.build.NumberCriteria;
import io.onedev.server.search.entity.build.VersionCriteria;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.DateUtils;
import io.onedev.server.util.Input;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.util.criteria.OrCriteria;
import io.onedev.server.web.WebConstants;
import io.onedev.server.web.WebSession;
import io.onedev.server.web.behavior.BuildQueryBehavior;
import io.onedev.server.web.behavior.WebSocketObserver;
import io.onedev.server.web.component.build.ParamValuesLabel;
import io.onedev.server.web.component.build.status.BuildStatusIcon;
import io.onedev.server.web.component.datatable.DefaultDataTable;
import io.onedev.server.web.component.datatable.selectioncolumn.SelectionColumn;
import io.onedev.server.web.component.entity.labels.EntityLabelsPanel;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.job.JobDefLink;
import io.onedev.server.web.component.link.ActionablePageLink;
import io.onedev.server.web.component.link.DropdownLink;
import io.onedev.server.web.component.menu.MenuItem;
import io.onedev.server.web.component.menu.MenuLink;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.modal.confirm.ConfirmModalPanel;
import io.onedev.server.web.component.orderedit.OrderEditPanel;
import io.onedev.server.web.component.savedquery.SavedQueriesClosed;
import io.onedev.server.web.component.savedquery.SavedQueriesOpened;
import io.onedev.server.web.component.stringchoice.StringMultiChoice;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.page.project.builds.detail.dashboard.BuildDashboardPage;
import io.onedev.server.web.util.Cursor;
import io.onedev.server.web.util.LoadableDetachableDataProvider;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
public abstract class BuildListPanel extends Panel {
	
	private final IModel<String> queryStringModel;
	
	private final boolean showJob;
	
	private final boolean showRef;
	
	private final int expectedCount;
	
	private final IModel<BuildQuery> queryModel = new LoadableDetachableModel<BuildQuery>() {

		@Override
		protected BuildQuery load() {
			return parse(queryStringModel.getObject(), getBaseQuery());
		}
		
	};
	
	private DataTable<Build, Void> buildsTable;
	
	private SelectionColumn<Build, Void> selectionColumn;
	
	private SortableDataProvider<Build, Void> dataProvider;	
	
	private TextField<String> queryInput;
	
	private WebMarkupContainer body;
	
	private Component saveQueryLink;
	
	private boolean querySubmitted = true;
	
	public BuildListPanel(String id, IModel<String> queryModel, boolean showJob, boolean showRef, int expectedCount) {
		super(id);
		this.queryStringModel = queryModel;
		this.showJob = showJob;
		this.showRef = showRef;
		this.expectedCount = expectedCount;
	}
	
	private BuildManager getBuildManager() {
		return OneDev.getInstance(BuildManager.class);
	}
	
	@Nullable
	private BuildQuery parse(@Nullable String queryString, BuildQuery baseQuery) {
		try {
			return BuildQuery.merge(baseQuery, BuildQuery.parse(getProject(), queryString, true, true));
		} catch (ExplicitException e) {
			error(e.getMessage());
			return null;
		} catch (Exception e) {
			info("Performing fuzzy query");
			try {
				EntityQuery.getProjectScopedNumber(getProject(), queryString);
				return BuildQuery.merge(baseQuery, 
						new BuildQuery(new NumberCriteria(getProject(), queryString, BuildQueryLexer.Is)));
			} catch (Exception e2) {
				List<Criteria<Build>> criterias = new ArrayList<>();
				criterias.add(new VersionCriteria("*" + queryString + "*"));
				criterias.add(new JobCriteria("*" + queryString + "*"));
				return BuildQuery.merge(baseQuery, new BuildQuery(new OrCriteria<Build>(criterias)));
			}
		}
	}
	
	@Override
	protected void onDetach() {
		queryStringModel.detach();
		queryModel.detach();
		super.onDetach();
	}
	
	@Nullable
	protected abstract Project getProject();

	protected BuildQuery getBaseQuery() {
		return new BuildQuery();
	}

	@Nullable
	protected PagingHistorySupport getPagingHistorySupport() {
		return null;
	}
	
	@Nullable
	protected QuerySaveSupport getQuerySaveSupport() {
		return null;
	}

	private GlobalBuildSetting getGlobalBuildSetting() {
		return OneDev.getInstance(SettingManager.class).getBuildSetting();
	}
	
	private void doQuery(AjaxRequestTarget target) {
		buildsTable.setCurrentPage(0);
		target.add(body);
		if (selectionColumn != null)
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
				if (event.getPayload() instanceof SavedQueriesClosed) {
					((SavedQueriesClosed) event.getPayload()).getHandler().add(this);
				}
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
				setVisible(SecurityUtils.getUser() != null && getQuerySaveSupport() != null);
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
		
		add(new MenuLink("operations") {

			@Override
			protected List<MenuItem> getMenuItems(FloatingPanel dropdown) {
				List<MenuItem> menuItems = new ArrayList<>();
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "取消选定的构建";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								String errorMessage = null;
								for (IModel<Build> each: selectionColumn.getSelections()) {
									Build build = each.getObject();
									if (build.isFinished()) {
										errorMessage = "构建 #" + build.getNumber() + " 已经完成了";
										break;
									} 
								}
								
								if (errorMessage != null) {
									getSession().error(errorMessage);
								} else {
									new ConfirmModalPanel(target) {
										
										@Override
										protected void onConfirm(AjaxRequestTarget target) {
											for (IModel<Build> each: selectionColumn.getSelections()) 
												OneDev.getInstance(JobManager.class).cancel(each.getObject());
											Session.get().success("提交的取消请求");
										}
										
										@Override
										protected String getConfirmMessage() {
											return "在下面键入 <code>yes</code> 下面取消选定的构建";
										}
										
										@Override
										protected String getConfirmInput() {
											return "yes"; 
										}
										
									};
								}
								
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
									tag.put("title", "请选择要取消的版本");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "重新运行选定的构建";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								String errorMessage = null;
								for (IModel<Build> each: selectionColumn.getSelections()) {
									Build build = each.getObject();
									if (!build.isFinished()) {
										errorMessage = "构建 #" + build.getNumber() + " 还没有完成";
										break;
									} 
								}
								
								if (errorMessage != null) {
									getSession().error(errorMessage);
								} else {
									new ConfirmModalPanel(target) {
										
										@Override
										protected void onConfirm(AjaxRequestTarget target) {
											for (IModel<Build> each: selectionColumn.getSelections()) { 
												Build build = each.getObject();
												OneDev.getInstance(JobManager.class).resubmit(build, "手动重新提交");
											}
											Session.get().success("已提交重新运行请求");
										}
										
										@Override
										protected String getConfirmMessage() {
											return "在下面键入 <code>yes</code> 下面重新运行选定的构建";
										}
										
										@Override
										protected String getConfirmInput() {
											return "yes"; 
										}
										
									};
								}
								
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
									tag.put("title", "请选择要重新运行的构建");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "删除选定的构建";
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
										Collection<Build> builds = new ArrayList<>();
										for (IModel<Build> each: selectionColumn.getSelections())
											builds.add(each.getObject());
										OneDev.getInstance(BuildManager.class).delete(builds);
										target.add(body);
										selectionColumn.getSelections().clear();
									}
									
									@Override
									protected String getConfirmMessage() {
										return "在下面键入 <code>yes</code> 下面删除选定的构建";
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
									tag.put("title", "请选择要删除的构建");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {
					
					@Override
					public String getLabel() {
						return "取消所有查询的构建";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								String errorMessage = null;
								for (Iterator<Build> it = (Iterator<Build>) dataProvider.iterator(0, buildsTable.getItemCount()); it.hasNext();) { 
									Build build = it.next();
									if (build.isFinished()) {
										errorMessage = "构建 #" + build.getNumber() + " 已经完成了";
										break;
									}
								}
								
								if (errorMessage != null) {
									getSession().error(errorMessage);
								} else {
									new ConfirmModalPanel(target) {
										
										@Override
										protected void onConfirm(AjaxRequestTarget target) {
											for (Iterator<Build> it = (Iterator<Build>) dataProvider.iterator(0, buildsTable.getItemCount()); it.hasNext();) 
												OneDev.getInstance(JobManager.class).cancel(it.next());
											Session.get().success("提交的取消请求");
										}
										
										@Override
										protected String getConfirmMessage() {
											return "在下面键入 <code>yes</code> 下面取消所有查询的构建";
										}
										
										@Override
										protected String getConfirmInput() {
											return "yes";
										}
										
									};
								}
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(buildsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有要取消的构建");
								}
							}
							
						};
					}
					
				});

				menuItems.add(new MenuItem() {
					
					@Override
					public String getLabel() {
						return "重新运行所有查询的构建";
					}
					
					@Override
					public WebMarkupContainer newLink(String id) {
						return new AjaxLink<Void>(id) {

							@SuppressWarnings("unchecked")
							@Override
							public void onClick(AjaxRequestTarget target) {
								dropdown.close();
								
								String errorMessage = null;
								for (Iterator<Build> it = (Iterator<Build>) dataProvider.iterator(0, buildsTable.getItemCount()); it.hasNext();) { 
									Build build = it.next();
									if (!build.isFinished()) {
										errorMessage = "构建 #" + build.getNumber() + " 还没有完成";
										break;
									}
								}
								
								if (errorMessage != null) {
									getSession().error(errorMessage);
								} else {
									new ConfirmModalPanel(target) {
										
										@Override
										protected void onConfirm(AjaxRequestTarget target) {
											for (Iterator<Build> it = (Iterator<Build>) dataProvider.iterator(0, buildsTable.getItemCount()); it.hasNext();) 
												OneDev.getInstance(JobManager.class).resubmit(it.next(), "手动重新提交");
											Session.get().success("已提交重新运行请求");
										}
										
										@Override
										protected String getConfirmMessage() {
											return "在下面键入 <code>yes</code> 以重新运行所有查询的构建";
										}
										
										@Override
										protected String getConfirmInput() {
											return "yes";
										}
										
									};
								}
								
							}
							
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(buildsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有要重新运行的构建");
								}
							}
							
						};
					}
					
				});
				
				menuItems.add(new MenuItem() {

					@Override
					public String getLabel() {
						return "删除所有查询的构建";
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
										Collection<Build> builds = new ArrayList<>();
										for (Iterator<Build> it = (Iterator<Build>) dataProvider.iterator(0, buildsTable.getItemCount()); it.hasNext();) {
											builds.add(it.next());
										}
										OneDev.getInstance(BuildManager.class).delete(builds);
										target.add(body);
										selectionColumn.getSelections().clear();
									}
									
									@Override
									protected String getConfirmMessage() {
										return "在下面键入 <code>yes</code> 下面删除所有查询的构建";
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
								setEnabled(buildsTable.getItemCount() != 0);
							}
							
							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								configure();
								if (!isEnabled()) {
									tag.put("disabled", "disabled");
									tag.put("title", "没有要删除的构建");
								}
							}
							
						};
					}
					
				});
				
				return menuItems;
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getProject() != null && SecurityUtils.canManageBuilds(getProject()));
			}
			
		});
		
		add(new ModalLink("listParams") {

			private List<String> listParams;
			
			@Override
			protected Component newContent(String id, ModalPanel modal) {
				Fragment fragment = new Fragment(id, "listParamsFrag", BuildListPanel.this);
				Form<?> form = new Form<Void>("form");
				listParams = getListParams();
				form.add(new StringMultiChoice("params", new IModel<Collection<String>>() {

					@Override
					public void detach() {
					}

					@Override
					public Collection<String> getObject() {
						return listParams;
					}

					@Override
					public void setObject(Collection<String> object) {
						listParams = new ArrayList<>(object);
					}
					
				}, new LoadableDetachableModel<Map<String, String>>() {

					@Override
					protected Map<String, String> load() {
						Map<String, String> choices = new LinkedHashMap<>();
						for (String fieldName: OneDev.getInstance(BuildParamManager.class).getParamNames(getProject()))
							choices.put(fieldName, fieldName);
						return choices;
					}
					
				}, false));
				
				form.add(new AjaxLink<Void>("close") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
					}
					
				});
				
				form.add(new AjaxButton("save") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						super.onSubmit(target, form);
						if (getProject() != null) {
							getProject().getBuildSetting().setListParams(listParams);
							OneDev.getInstance(ProjectManager.class).save(getProject());
						} else {
							getGlobalBuildSetting().setListParams(listParams);
							OneDev.getInstance(SettingManager.class).saveBuildSetting(getGlobalBuildSetting());
						}
						setResponsePage(getPage().getClass(), getPage().getPageParameters());
					}
					
				});
				
				form.add(new AjaxLink<Void>("useDefault") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
						getProject().getBuildSetting().setListParams(null);
						OneDev.getInstance(ProjectManager.class).save(getProject());
						target.add(body);
					}
					
				}.setVisible(getProject() != null && getProject().getBuildSetting().getListParams(false) != null));
				
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
					}
					
				});

				fragment.add(form);
				
				return fragment;
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.isAdministrator() 
						|| getProject() != null && SecurityUtils.canManageBuilds(getProject()));
			}
			
		});		
		
		add(new DropdownLink("orderBy") {

			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				List<String> orderFields = new ArrayList<>(Build.ORDER_FIELDS.keySet());
				if (getProject() != null)
					orderFields.remove(Build.NAME_PROJECT);
				
				return new OrderEditPanel<Build>(id, orderFields, new IModel<List<EntitySort>> () {

					@Override
					public void detach() {
					}

					@Override
					public List<EntitySort> getObject() {
						BuildQuery query = parse(queryStringModel.getObject(), new BuildQuery());
						BuildListPanel.this.getFeedbackMessages().clear();
						if (query != null) 
							return query.getSorts();
						else
							return new ArrayList<>();
					}

					@Override
					public void setObject(List<EntitySort> object) {
						BuildQuery query = parse(queryStringModel.getObject(), new BuildQuery());
						BuildListPanel.this.getFeedbackMessages().clear();
						if (query == null)
							query = new BuildQuery();
						query.getSorts().clear();
						query.getSorts().addAll(object);
						queryStringModel.setObject(query.toString());
						AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class); 
						target.add(queryInput);
						doQuery(target);
					}
					
				});
			}
			
		});	
		
		queryInput = new TextField<String>("input", queryStringModel);
		queryInput.add(new BuildQueryBehavior(new AbstractReadOnlyModel<Project>() {

			@Override
			public Project getObject() {
				return getProject();
			}
			
		}, true, true, true) {
			
			@Override
			protected void onInput(AjaxRequestTarget target, String inputContent) {
				BuildListPanel.this.getFeedbackMessages().clear();
				querySubmitted = StringUtils.trimToEmpty(queryStringModel.getObject())
						.equals(StringUtils.trimToEmpty(inputContent));
				target.add(saveQueryLink);
			}

			@Override
			protected List<String> getHints(TerminalExpect terminalExpect) {
				List<String> hints = super.getHints(terminalExpect);
				hints.add("模糊查询的自由输入 number/version/job");
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
				BuildListPanel.this.getFeedbackMessages().clear();
				doQuery(target);
			}
			
		});
		add(queryForm);
		
		dataProvider = new LoadableDetachableDataProvider<Build, Void>() {

			@Override
			public Iterator<? extends Build> iterator(long first, long count) {
				try {
					return getBuildManager().query(getProject(), queryModel.getObject(), 
							(int)first, (int)count).iterator();
				} catch (ExplicitException e) {
					error(e.getMessage());
					return new ArrayList<Build>().iterator();
				}
			}

			@Override
			public long calcSize() {
				BuildQuery query = queryModel.getObject();
				if (query != null) {
					try {
						return getBuildManager().count(getProject(), query.getCriteria());
					} catch (ExplicitException e) {
						error(e.getMessage());
					}
				} 
				return 0;
			}

			@Override
			public IModel<Build> model(Build object) {
				Long buildId = object.getId();
				return new LoadableDetachableModel<Build>() {

					@Override
					protected Build load() {
						return getBuildManager().load(buildId);
					}
					
				};
			}
			
		};
		
		if (expectedCount != 0 && expectedCount != dataProvider.size())
			warn("由于权限策略，某些构建可能会被隐藏");
		
		body = new WebMarkupContainer("body");
		add(body.setOutputMarkupId(true));
		
		body.add(new FencedFeedbackPanel("feedback", this));
		
		List<IColumn<Build, Void>> columns = new ArrayList<>();
		
		if (getProject() != null && SecurityUtils.canManageBuilds(getProject())) 
			columns.add(selectionColumn = new SelectionColumn<Build, Void>());
		
		columns.add(new AbstractColumn<Build, Void>(Model.of("Build")) {

			@Override
			public String getCssClass() {
				return "build";
			}

			@Override
			public void populateItem(Item<ICellPopulator<Build>> cellItem, String componentId, IModel<Build> rowModel) {
				Fragment fragment = new Fragment(componentId, "buildFrag", BuildListPanel.this);
				Build build = rowModel.getObject();
				Long buildId = build.getId();
				
				WebMarkupContainer link = new ActionablePageLink("link", 
						BuildDashboardPage.class, BuildDashboardPage.paramsOf(build)) {

					@Override
					protected void doBeforeNav(AjaxRequestTarget target) {
						OddEvenItem<?> row = cellItem.findParent(OddEvenItem.class);
						Cursor cursor = new Cursor(queryModel.getObject().toString(), (int)buildsTable.getItemCount(), 
								(int)buildsTable.getCurrentPage() * WebConstants.PAGE_SIZE + row.getIndex(), getProject());
						WebSession.get().setBuildCursor(cursor);								

						String directUrlAfterDelete = RequestCycle.get().urlFor(
								getPage().getClass(), getPage().getPageParameters()).toString();
						WebSession.get().setRedirectUrlAfterDelete(Build.class, directUrlAfterDelete);
					}
					
				};
				link.add(new BuildStatusIcon("icon", new LoadableDetachableModel<Status>() {

					@Override
					protected Status load() {
						return getBuildManager().load(buildId).getStatus();
					}
					
				}));
				link.add(new Label("text", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						Build build = rowModel.getObject();
						StringBuilder builder = new StringBuilder();
						
						Project currentProject = null;
						if (getPage() instanceof ProjectPage)
							currentProject = ((ProjectPage) getPage()).getProject();
						if (currentProject == null || !currentProject.equals(build.getProject()))
							builder.append(build.getProject().getPath());
						builder.append("#" + build.getNumber());
						if (build.getVersion() != null)
							builder.append(" (" + build.getVersion() + ")");
						return builder.toString();
					}
					
				}));
				fragment.add(link);
				
				fragment.add(new EntityLabelsPanel<BuildLabel>("labels", rowModel));
				
				fragment.add(newBuildObserver(buildId));
				fragment.setOutputMarkupId(true);
				cellItem.add(fragment);
			}
		});
		
		if (showJob) {
			columns.add(new AbstractColumn<Build, Void>(Model.of(Build.NAME_JOB)) {
	
				@Override
				public String getCssClass() {
					return "job";
				}
	
				@Override
				public void populateItem(Item<ICellPopulator<Build>> cellItem, String componentId,
						IModel<Build> rowModel) {
					Build build = rowModel.getObject();
					if (SecurityUtils.canReadCode(build.getProject())) {
						Fragment fragment = new Fragment(componentId, "linkFrag", BuildListPanel.this);
						Link<Void> link = new JobDefLink("link", build.getCommitId(), build.getJobName()) {
	
							@Override
							protected Project getProject() {
								return rowModel.getObject().getProject();
							}
	
							@Override
							protected void onConfigure() {
								super.onConfigure();
								setEnabled(isEnabled() && build.getJob() != null);
							}
							
						};
						link.add(new Label("label", build.getJobName()));
						fragment.add(link);
						cellItem.add(fragment);
					} else {
						cellItem.add(new Label(componentId, build.getJobName()));
					}
				}
			});
		}
		
		if (showRef) {
			columns.add(new AbstractColumn<Build, Void>(Model.of("Branch/Tag")) {
	
				@Override
				public String getCssClass() {
					return "branch-tag d-none d-lg-table-cell";
				}
	
				@Override
				public void populateItem(Item<ICellPopulator<Build>> cellItem, String componentId,
						IModel<Build> rowModel) {
					Build build = rowModel.getObject();
					if (SecurityUtils.canReadCode(build.getProject())) {
						if (build.getBranch() != null) {
							Fragment fragment = new Fragment(componentId, "linkFrag", BuildListPanel.this);
							PageParameters params = ProjectBlobPage.paramsOf(build.getProject(), 
									new BlobIdent(build.getBranch(), null, FileMode.TREE.getBits()));
							Link<Void> link = new BookmarkablePageLink<Void>("link", ProjectBlobPage.class, params);
							link.add(new Label("label", build.getBranch()));
							fragment.add(link);
							cellItem.add(fragment);
						} else if (build.getTag() != null) {
							Fragment fragment = new Fragment(componentId, "linkFrag", BuildListPanel.this);
							PageParameters params = ProjectBlobPage.paramsOf(build.getProject(), 
									new BlobIdent(build.getTag(), null, FileMode.TREE.getBits()));
							Link<Void> link = new BookmarkablePageLink<Void>("link", ProjectBlobPage.class, params);
							link.add(new Label("label", build.getTag()));
							fragment.add(link);
							cellItem.add(fragment);
						} else { 
							cellItem.add(new Label(componentId, "<i>n/a</i>").setEscapeModelStrings(false));
						}
					} else {
						if (build.getBranch() != null) 
							cellItem.add(new Label(componentId, build.getBranch()));
						else if (build.getTag() != null)
							cellItem.add(new Label(componentId, build.getTag()));
						else 
							cellItem.add(new Label(componentId, "<i>n/a</i>").setEscapeModelStrings(false));
					}
				}
			});
		}
		
		for (String paramName: getListParams()) {
			columns.add(new AbstractColumn<Build, Void>(Model.of(paramName)) {

				@Override
				public String getCssClass() {
					return "param d-none d-xl-table-cell text-break";
				}

				@Override
				public void populateItem(Item<ICellPopulator<Build>> cellItem, String componentId, IModel<Build> rowModel) {
					Build build = rowModel.getObject();
					Input param = build.getParamInputs().get(paramName);
					if (param != null && build.isParamVisible(paramName))
						cellItem.add(new ParamValuesLabel(componentId, param));
					else
						cellItem.add(new Label(componentId, "<i>未指定</i>").setEscapeModelStrings(false));
				}
				
			});
		}	
		
		columns.add(new AbstractColumn<Build, Void>(Model.of("Last Update")) {

			@Override
			public String getCssClass() {
				return "date d-none d-xl-table-cell";
			}

			@Override
			public void populateItem(Item<ICellPopulator<Build>> cellItem, String componentId, IModel<Build> rowModel) {
				Build build = rowModel.getObject();
				Long buildId = build.getId();

				Fragment fragment = new Fragment(componentId, "dateFrag", BuildListPanel.this);
				fragment.add(new Label("name", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return rowModel.getObject().getStatus().toString();
					}
					
				}) {

					@Override
					protected void onInitialize() {
						super.onInitialize();
						add(newBuildObserver(buildId));
						setOutputMarkupId(true);
					}
					
				});
				fragment.add(new Label("date", new LoadableDetachableModel<String>() {

					@Override
					protected String load() {
						return DateUtils.formatAge(rowModel.getObject().getStatusDate());
					}
					
				}));
				fragment.add(newBuildObserver(buildId));
				fragment.setOutputMarkupId(true);
				cellItem.add(fragment);
			}
		});		
		
		body.add(buildsTable = new DefaultDataTable<Build, Void>("builds", columns, dataProvider, 
				WebConstants.PAGE_SIZE, getPagingHistorySupport()));
		
		body.add(new WebMarkupContainer("tips") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.getUser() != null && getProject() != null && SecurityUtils.canReadCode(getProject()));
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	private List<String> getListParams() {
		if (getProject() != null)
			return getProject().getBuildSetting().getListParams(true);
		else
			return getGlobalBuildSetting().getListParams();
	}
	
	private WebSocketObserver newBuildObserver(Long buildId) {
		return new WebSocketObserver() {
			
			@Override
			public void onObservableChanged(IPartialPageRequestHandler handler) {
				handler.add(component);
			}
			
			@Override
			public Collection<String> getObservables() {
				return Sets.newHashSet(Build.getWebSocketObservable(buildId));
			}
			
		};
	}
	
}
