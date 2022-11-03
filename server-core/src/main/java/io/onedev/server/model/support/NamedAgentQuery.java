package io.onedev.server.model.support;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.AgentQuery;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class NamedAgentQuery implements NamedQuery {
	
	private static final long serialVersionUID = 1L;

	private String name;
	
	private String query;
	
	public NamedAgentQuery(String name, String query) {
		this.name = name;
		this.query = query;
	}
	
	public NamedAgentQuery() {
	}

	@Editable
	@NotEmpty
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(placeholder="所有")
	@AgentQuery(forExecutor=false)
	@Override
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}