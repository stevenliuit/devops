package io.onedev.server.buildspec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputCompletion;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.buildspec.job.EnvVar;
import io.onedev.server.util.validation.annotation.DnsName;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.SuggestionProvider;

@Editable
public class Service implements NamedElement, Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	
	private String image;
	
	private String arguments;
	
	private List<EnvVar> envVars = new ArrayList<>();
	
	private String readinessCheckCommand;
	
	private int cpuRequirement = 250;
	
	private int memoryRequirement = 256;
	
	@Editable(order=100, description="指定服务名称，作为访问服务的主机名")
	@SuggestionProvider("getNameSuggestions")
	@DnsName
	@NotEmpty
	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@SuppressWarnings("unused")
	private static List<InputCompletion> getNameSuggestions(InputStatus status) {
		BuildSpec buildSpec = BuildSpec.get();
		if (buildSpec != null) {
			List<String> candidates = new ArrayList<>(buildSpec.getServiceMap().keySet());
			buildSpec.getServices().forEach(it->candidates.remove(it.getName()));
			return BuildSpec.suggestOverrides(candidates, status);
		}
		return new ArrayList<>();
	}

	@Editable(order=200, description="Specify docker image of the service")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Editable(order=220, description="（可选）指定要在image上方运行的参数")
	@Interpolative(variableSuggester="suggestVariables")
	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	@Editable(order=300, name="环境变量", description="可选择指定服务的环境变量")
	public List<EnvVar> getEnvVars() {
		return envVars;
	}

	public void setEnvVars(List<EnvVar> envVars) {
		this.envVars = envVars;
	}

	@Editable(order=400, description="指定命令以检查服务的就绪情况. 此命令将由 Windows image上的 cmd.exe 解释,"
			+ " 并通过 Linux image上的 shell. 它将重复执行，直到返回一个零代码表示服务就绪")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getReadinessCheckCommand() {
		return readinessCheckCommand;
	}

	public void setReadinessCheckCommand(String readinessCheckCommand) {
		this.readinessCheckCommand = readinessCheckCommand;
	}
	
	@Editable(order=10000, name="CPU 要求", group="更多设置", description="以毫秒为单位指定服务的 CPU 要求. "
			+ "1000 毫秒表示单个 CPU 内核")
	public int getCpuRequirement() {
		return cpuRequirement;
	}

	public void setCpuRequirement(int cpuRequirement) {
		this.cpuRequirement = cpuRequirement;
	}

	@Editable(order=10100, group="更多设置", description="以兆字节指定服务的内存需求")
	public int getMemoryRequirement() {
		return memoryRequirement;
	}

	public void setMemoryRequirement(int memoryRequirement) {
		this.memoryRequirement = memoryRequirement;
	}
	
	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, false, false, false);
	}
	
	public Map<String, Serializable> toMap() {
		Map<String, Serializable> serviceMap = new HashMap<>();
		
		serviceMap.put("name", getName());
		serviceMap.put("image", getImage());
		serviceMap.put("readinessCheckCommand", getReadinessCheckCommand());
		serviceMap.put("cpuRequirement", getCpuRequirement());
		serviceMap.put("memoryRequirement", getMemoryRequirement());
		serviceMap.put("arguments", getArguments());
		Map<String, String> envVars = new HashMap<>();
		for (EnvVar var: getEnvVars())
			envVars.put(var.getName(), var.getValue());
		serviceMap.put("envVars", (Serializable) envVars);
		
		return serviceMap;
	}
	
}
