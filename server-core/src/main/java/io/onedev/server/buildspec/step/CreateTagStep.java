package io.onedev.server.buildspec.step;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.Multiline;

@Editable(name="创建标签", order=300)
public class CreateTagStep extends ServerSideStep {

	private static final long serialVersionUID = 1L;
	
	private String tagName;
	
	private String tagMessage;
	
	@Editable(order=1000, description="指定标签名称")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	@Editable(order=1050, description="可选地指定标签的消息")
	@Multiline
	@Interpolative(variableSuggester="suggestVariables")
	public String getTagMessage() {
		return tagMessage;
	}

	public void setTagMessage(String tagMessage) {
		this.tagMessage = tagMessage;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, true, true, false);
	}

	@Override
	public Map<String, byte[]> run(Build build, File inputDir, TaskLogger logger) {
		PersonIdent taggerIdent = OneDev.getInstance(UserManager.class).getSystem().asPerson();
		Project project = build.getProject();
		String tagName = getTagName();

		if (build.canCreateTag(tagName)) {
			PGPSecretKeyRing signingKey = OneDev.getInstance(SettingManager.class)
					.getGpgSetting().getSigningKey();
			Ref tagRef = project.getTagRef(tagName);
			if (tagRef != null) {
				OneDev.getInstance(ProjectManager.class).deleteTag(project, tagName);
				project.createTag(tagName, build.getCommitHash(), taggerIdent, getTagMessage(), signingKey);
			} else {
				project.createTag(tagName, build.getCommitHash(), taggerIdent, getTagMessage(), signingKey);
			}
		} else {
			throw new ExplicitException("This build is not authorized to create tag '" + tagName + "'");
		}
		
		return null;
	}

}
