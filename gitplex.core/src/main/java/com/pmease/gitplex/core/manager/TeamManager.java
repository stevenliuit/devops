package com.pmease.gitplex.core.manager;

import javax.annotation.Nullable;

import com.pmease.commons.hibernate.dao.EntityDao;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Team;

public interface TeamManager extends EntityDao<Team> {
	/**
	 * Save specified team in specified organization
	 * 
	 * @param team
	 * 			team to save
	 * @param oldName
	 * 			in case of rename, this parameter should hold the original name 
	 * 			when above team object is initially loaded to ensure database 
	 * 			integrity. Use <tt>null</tt> if original name does not exist, 
	 * 			or the name is not changed
	 */
	void save(Team team, @Nullable String oldName);
	
	void delete(Team team);
	
	@Nullable
	Team find(Account organization, String name);
}
