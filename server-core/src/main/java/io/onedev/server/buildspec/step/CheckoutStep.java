package io.onedev.server.buildspec.step;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.k8shelper.CheckoutFacade;
import io.onedev.k8shelper.StepFacade;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.job.gitcredential.DefaultCredential;
import io.onedev.server.buildspec.job.gitcredential.GitCredential;
import io.onedev.server.buildspec.param.ParamCombination;
import io.onedev.server.model.Build;
import io.onedev.server.util.validation.annotation.SafePath;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable(order=50, name="检出代码")
public class CheckoutStep extends Step {

	private static final long serialVersionUID = 1L;

	private GitCredential cloneCredential = new DefaultCredential();
	
	private boolean withLfs;
	
	private boolean withSubmodules;
	
	private Integer cloneDepth;
	
	private String checkoutPath;
	
	@Editable(order=100, description="默认情况下，代码通过自动生成的凭证克隆, "
			+ "仅对当前项目具有读取权限. 如果工作需要 <a href='$docRoot/pages/push-in-job.md' target='_blank'>将代码推送到服务器</a>, 或想要 "
			+ "<a href='$docRoot/pages/clone-submodules-via-ssh.md' target='_blank'>克隆私有子模块</a>, 您应该在此处提供具有适当权限的自定义凭据")
	@NotNull
	public GitCredential getCloneCredential() {
		return cloneCredential;
	}

	public void setCloneCredential(GitCredential cloneCredential) {
		this.cloneCredential = cloneCredential;
	}

	@Editable(order=120, name="检索 LFS 文件", description="选中此项以检索 Git LFS 文件")
	public boolean isWithLfs() {
		return withLfs;
	}

	public void setWithLfs(boolean withLfs) {
		this.withLfs = withLfs;
	}

	@Editable(order=180, name="检索子模块", description="选中此项以检索子模块")
	public boolean isWithSubmodules() {
		return withSubmodules;
	}

	public void setWithSubmodules(boolean withSubmodules) {
		this.withSubmodules = withSubmodules;
	}

	@Editable(order=200, description="可选择按顺序指定浅克隆的深度 "
			+ "加快源检索")
	public Integer getCloneDepth() {
		return cloneDepth;
	}

	public void setCloneDepth(Integer cloneDepth) {
		this.cloneDepth = cloneDepth;
	}

	@Editable(order=300, placeholder="工作空间", description="（可选）指定相对于的路径 "
			+ "<a href='$docRoot/pages/concepts.md#job-workspace'>工作区</a> 将代码克隆. "
			+ "留空以使用作业工作区本身")
	@Interpolative(variableSuggester="suggestVariables")
	@SafePath
	public String getCheckoutPath() {
		return checkoutPath;
	}

	public void setCheckoutPath(String checkoutPath) {
		this.checkoutPath = checkoutPath;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
	@Override
	public StepFacade getFacade(Build build, String jobToken, ParamCombination paramCombination) {
		return new CheckoutFacade(cloneDepth!=null?cloneDepth:0, withLfs, withSubmodules, 
				cloneCredential.newCloneInfo(build, jobToken), checkoutPath);
	}

}
