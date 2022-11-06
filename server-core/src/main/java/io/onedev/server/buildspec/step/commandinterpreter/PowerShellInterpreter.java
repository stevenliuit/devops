package io.onedev.server.buildspec.step.commandinterpreter;

import java.util.List;

import javax.validation.constraints.Size;

import io.onedev.k8shelper.CommandFacade;
import io.onedev.k8shelper.PowerShellFacade;
import io.onedev.server.util.validation.annotation.Code;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable(order=300, name="PowerShell")
public class PowerShellInterpreter extends Interpreter {

	private static final long serialVersionUID = 1L;

	@Editable(order=110, description="指定要执行的 PowerShell 命令 "
			+ "在下面 <a href='$docRoot/pages/concepts.md#job-workspace' target='_blank'>工作区</a>.<br>"
			+ "<b class='text-warning'>NOTE: </b>系统检查脚本的退出代码以确定步骤是否成功. "
			+ "由于 PowerShell 总是以 0 退出，即使存在脚本错误, 你应该处理脚本中的错误 "
			+ "并以非零代码退出，或添加行 <code>$ErrorActionPreference = &quot;Stop&quot;</code> 在脚本的开头<br>")
	@Interpolative
	@Code(language=Code.POWER_SHELL, variableProvider="suggestVariables")
	@Size(min=1, message="不能为空")
	@Override
	public List<String> getCommands() {
		return super.getCommands();
	}

	@Override
	public void setCommands(List<String> commands) {
		super.setCommands(commands);
	}
	
	@Override
	public CommandFacade getExecutable(String image, boolean useTTY) {
		return new PowerShellFacade(image, getCommands(), useTTY);
	}

}
