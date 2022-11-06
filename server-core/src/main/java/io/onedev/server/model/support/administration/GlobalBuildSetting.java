package io.onedev.server.model.support.administration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.onedev.server.model.support.build.NamedBuildQuery;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class GlobalBuildSetting implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private List<NamedBuildQuery> namedQueries = new ArrayList<>();
	
	private List<String> listParams = new ArrayList<>();
	
	public GlobalBuildSetting() {
		namedQueries.add(new NamedBuildQuery("All", null));
		namedQueries.add(new NamedBuildQuery("成功的", "successful"));
		namedQueries.add(new NamedBuildQuery("失败的", "failed"));
		namedQueries.add(new NamedBuildQuery("取消", "cancelled"));
		namedQueries.add(new NamedBuildQuery("超时", "timed out"));
		namedQueries.add(new NamedBuildQuery("运行中", "running"));
		namedQueries.add(new NamedBuildQuery("等待中", "waiting"));
		namedQueries.add(new NamedBuildQuery("待办的", "pending"));
		namedQueries.add(new NamedBuildQuery("最近构建", "\"Submit Date\" is since \"last week\""));		
	}
	
	public List<NamedBuildQuery> getNamedQueries() {
		return namedQueries;
	}

	public void setNamedQueries(List<NamedBuildQuery> namedQueries) {
		this.namedQueries = namedQueries;
	}
	
	public List<String> getListParams() {
		return listParams;
	}

	public void setListParams(List<String> listParams) {
		this.listParams = listParams;
	}

	@Nullable
	public NamedBuildQuery getNamedQuery(String name) {
		for (NamedBuildQuery namedQuery: getNamedQueries()) {
			if (namedQuery.getName().equals(name))
				return namedQuery;
		}
		return null;
	}
	
}
