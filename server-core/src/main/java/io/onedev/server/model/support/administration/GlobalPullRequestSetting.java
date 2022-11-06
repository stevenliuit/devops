package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.onedev.server.model.support.pullrequest.NamedPullRequestQuery;

public class GlobalPullRequestSetting implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private List<NamedPullRequestQuery> namedQueries = new ArrayList<>();
	
	public GlobalPullRequestSetting() {
		namedQueries.add(new NamedPullRequestQuery("打开", "open"));
		namedQueries.add(new NamedPullRequestQuery("由我审核", "open and to be reviewed by me"));
		namedQueries.add(new NamedPullRequestQuery("由我改变", "open and submitted by me and someone requested for changes"));
		namedQueries.add(new NamedPullRequestQuery("我要求更改", "requested for changes by me"));
		namedQueries.add(new NamedPullRequestQuery("经我批准", "approved by me"));
		namedQueries.add(new NamedPullRequestQuery("由我提交", "submitted by me"));
		namedQueries.add(new NamedPullRequestQuery("最近提交", "\"Submit Date\" is since \"last week\""));
		namedQueries.add(new NamedPullRequestQuery("最近更新了", "\"Update Date\" is since \"last week\""));
		namedQueries.add(new NamedPullRequestQuery("关闭", "merged or discarded"));
		namedQueries.add(new NamedPullRequestQuery("所有", null));
	}
	
	public List<NamedPullRequestQuery> getNamedQueries() {
		return namedQueries;
	}

	public void setNamedQueries(List<NamedPullRequestQuery> namedQueries) {
		this.namedQueries = namedQueries;
	}
	
	@Nullable
	public NamedPullRequestQuery getNamedQuery(String name) {
		for (NamedPullRequestQuery namedQuery: getNamedQueries()) {
			if (namedQuery.getName().equals(name))
				return namedQuery;
		}
		return null;
	}
	
}
