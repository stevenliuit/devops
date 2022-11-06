package io.onedev.server.buildspec.step;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.LockUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.model.Build;
import io.onedev.server.storage.StorageManager;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.util.validation.annotation.SafePath;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.Patterns;

@Editable(order=1050, name="发布工件")
public class PublishArtifactStep extends ServerSideStep {

	private static final long serialVersionUID = 1L;

	private String sourcePath;
	
	private String artifacts;
	
	@Editable(order=50, name="从路径", placeholder="工作空间", description="（可选）指定相对于的路径"
			+ " <a href='$docRoot/pages/concepts.md#job-workspace'>工作区</a> 去发布 "
			+ "来自的工件. 保留为空以使用作业工作区本身")
	@Interpolative(variableSuggester="suggestVariables")
	@SafePath
	@Override
	public String getSourcePath() {
		return sourcePath;
	}
	
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}
	
	@Editable(order=100, description="指定要作为作业工件发布的文件（相对于上面指定的源路径）. 使用 * 或者 ? 用于模式匹配")
	@Interpolative(variableSuggester="suggestVariables")
	@Patterns(path=true)
	@NotEmpty
	public String getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(String artifacts) {
		this.artifacts = artifacts;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, true, true, false);
	}

	@Override
	protected PatternSet getFiles() {
		return PatternSet.parse(getArtifacts());
	}

	@Override
	public Map<String, byte[]> run(Build build, File inputDir, TaskLogger jobLogger) {
		LockUtils.write(build.getArtifactsLockKey(), new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				File artifactsDir = build.getArtifactsDir();
				OneDev.getInstance(StorageManager.class).initArtifactsDir(build.getProject().getId(), build.getNumber());
				FileUtils.copyDirectory(inputDir, artifactsDir);
				return null;
			}
			
		});
		return null;
	}

}
