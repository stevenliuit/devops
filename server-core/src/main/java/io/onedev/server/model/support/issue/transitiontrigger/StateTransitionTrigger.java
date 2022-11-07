package io.onedev.server.model.support.issue.transitiontrigger;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Size;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.support.issue.StateSpec;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.IssueQuery;
import io.onedev.server.web.editable.annotation.OmitName;

@Editable(order=550, name="其他问题的状态转移到")
public class StateTransitionTrigger extends TransitionTrigger {

	private static final long serialVersionUID = 1L;
	
	private List<String> states = new ArrayList<>();
	
	@Editable(order=100)
	@OmitName
	@ChoiceProvider("getStateChoices")
	@Size(min=1, message="至少需要指定一种状态")
	public List<String> getStates() {
		return states;
	}

	public void setStates(List<String> states) {
		this.states = states;
	}

	@SuppressWarnings("unused")
	private static List<String> getStateChoices() {
		List<String> stateNames = new ArrayList<>();
		for (StateSpec state: OneDev.getInstance(SettingManager.class).getIssueSetting().getStateSpecs())
			stateNames.add(state.getName());
		return stateNames;
	}
	
	@Editable(order=1000, name="适用问题", placeholder="所有", description="（可选）指定适用于此过渡的问题。 为所有问题留空")
	@IssueQuery(withOrder = false, withCurrentIssueCriteria = true)
	@Override
	public String getIssueQuery() {
		return super.getIssueQuery();
	}

	public void setIssueQuery(String issueQuery) {
		super.setIssueQuery(issueQuery);
	}

	@Override
	public String getDescription() {
		return "问题的状态转换为 " + states;
	}
	
}
