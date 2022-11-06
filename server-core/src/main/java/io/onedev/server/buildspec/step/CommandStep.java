package io.onedev.server.buildspec.step;

import javax.validation.constraints.NotNull;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.k8shelper.StepFacade;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.param.ParamCombination;
import io.onedev.server.buildspec.step.commandinterpreter.DefaultInterpreter;
import io.onedev.server.buildspec.step.commandinterpreter.Interpreter;
import io.onedev.server.model.Build;
import io.onedev.server.util.EditContext;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.ShowCondition;

@Editable(order=100, name="执行命令")
public class CommandStep extends Step {

	private static final long serialVersionUID = 1L;
	
	public static final String USE_TTY_HELP = "许多命令以 ANSI 颜色打印输出 "
			+ "TTY 模式有助于轻松识别问题. 但是在此模式下运行的某些命令可能 "
			+ "等待用户输入导致构建挂起. 这通常可以通过在命令中添加额外的选项来解决";

	private boolean runInContainer = true;
	
	private String image;
	
	private Interpreter interpreter = new DefaultInterpreter();
	
	private boolean useTTY;
	
	@Editable(order=50, description="是否在容器内运行此步骤")
	public boolean isRunInContainer() {
		return runInContainer;
	}

	public void setRunInContainer(boolean runInContainer) {
		this.runInContainer = runInContainer;
	}

	@SuppressWarnings("unused")
	private static boolean isRunInContainerEnabled() {
		return (boolean) EditContext.get().getInputValue("runInContainer");
	}
	
	@Editable(order=100, description="指定容器镜像执行里面的命令")
	@ShowCondition("isRunInContainerEnabled")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
	@Editable(order=110)
	@NotNull
	public Interpreter getInterpreter() {
		return interpreter;
	}

	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Editable(order=10000, name="启用 TTY 模式", description=USE_TTY_HELP)
	@ShowCondition("isRunInContainerEnabled")
	public boolean isUseTTY() {
		return useTTY;
	}

	public void setUseTTY(boolean useTTY) {
		this.useTTY = useTTY;
	}

	@Override
	public StepFacade getFacade(Build build, String jobToken, ParamCombination paramCombination) {
		return getInterpreter().getExecutable(isRunInContainer()?getImage():null, isUseTTY());
	}
	
}
