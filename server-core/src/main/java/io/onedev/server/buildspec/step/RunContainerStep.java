package io.onedev.server.buildspec.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.k8shelper.RunContainerFacade;
import io.onedev.k8shelper.StepFacade;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.job.EnvVar;
import io.onedev.server.buildspec.param.ParamCombination;
import io.onedev.server.model.Build;
import io.onedev.server.util.validation.annotation.SafePath;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable(order=150, name="运行 Docker 容器", description="运行指定的 docker 容器。 访问文件 "
		+ "工作区, 要么使用环境变量 <tt>JOB_WORKSPACE</tt>, 或指定卷安装. "
		+ "<b class='text-warning'>NOTE:</b> 如果此步骤使用 Kubernetes 执行程序运行, 选项 "
		+ "<code>安装容器sock</code> 必须启用该执行程序")
public class RunContainerStep extends Step {

	private static final long serialVersionUID = 1L;

	private String image;
	
	private String args;
	
	private List<EnvVar> envVars = new ArrayList<>();

	private String workingDir;
	
	private List<VolumeMount> volumeMounts = new ArrayList<>(); 
	
	private boolean useTTY;
	
	@Override
	public StepFacade getFacade(Build build, String jobToken, ParamCombination paramCombination) {
		Map<String, String> envMap = new HashMap<>();
		for (EnvVar var: getEnvVars())
			envMap.put(var.getName(), var.getValue());
		Map<String, String> mountMap = new HashMap<>();
		for (VolumeMount mount: getVolumeMounts())
			mountMap.put(mount.getSourcePath(), mount.getTargetPath());
		return new RunContainerFacade(getImage(), getArgs(), envMap, getWorkingDir(), mountMap, isUseTTY());
	}

	@Editable(order=100, description="指定要运行的容器映像. <b class='text-warning'>NOTE:</b> 如果步骤由 kubernetes executor 执行，则容器中必须存在 shell"
			+ ", 因为系统需要拦截入口点以使步骤容器在 pod 中顺序执行")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Editable(order=200, name="Arguments", description="指定用空格分隔的容器参数. "
			+ "应引用包含空格的单个参数")
	@Interpolative(variableSuggester="suggestVariables")
	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	@Editable(order=200, name="工作目录", description="可选择指定容器的工作目录. "
			+ "留空以使用容器的默认工作目录")
	@SafePath
	@Interpolative(variableSuggester="suggestVariables")
	@Nullable
	public String getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	@Editable(order=400, name="环境变量", description="可选择指定环境 "
			+ "容器变量")
	public List<EnvVar> getEnvVars() {
		return envVars;
	}

	public void setEnvVars(List<EnvVar> envVars) {
		this.envVars = envVars;
	}

	@Editable(order=500, description="可选择将作业工作区下的目录或文件挂载到容器中")
	public List<VolumeMount> getVolumeMounts() {
		return volumeMounts;
	}

	public void setVolumeMounts(List<VolumeMount> volumeMounts) {
		this.volumeMounts = volumeMounts;
	}

	@Editable(order=10000, name="启用 TTY 模式", description="许多命令在 TTY 模式下使用 ANSI 颜色打印输出，以帮助轻松识别问题."
			+ " 但是，在此模式下运行的某些命令可能会等待用户输入导致构建挂起."
			+ " 这通常可以通过在命令中添加额外的选项来解决")
	public boolean isUseTTY() {
		return useTTY;
	}

	public void setUseTTY(boolean useTTY) {
		this.useTTY = useTTY;
	}

	static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
}
