package io.onedev.server.model.support.administration;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.server.web.editable.annotation.Editable;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

@Editable
public class PerformanceSetting implements Serializable {
	
	private static final long serialVersionUID = 1;
	
	private static final Logger logger=  LoggerFactory.getLogger(PerformanceSetting.class);
	
	private int cpuIntensiveTaskConcurrency;
	
	private int serverJobExecutorCpuQuota;
	
	private int serverJobExecutorMemoryQuota;
	
	private int maxGitLFSFileSize = 4096;  
	
	private int maxUploadFileSize = 20;
	
	private int maxCodeSearchEntries = 100;
	
	public PerformanceSetting() {
		try {
			HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
			cpuIntensiveTaskConcurrency = hardware.getProcessor().getLogicalProcessorCount();
			serverJobExecutorCpuQuota = hardware.getProcessor().getLogicalProcessorCount()*1000;
			serverJobExecutorMemoryQuota = (int) (hardware.getMemory().getTotal()/1024/1024);				
		} catch (Exception e) {
			logger.debug("Error calling oshi", e);
			logger.warn("Unable to call oshi to set default performance setting. Assume some arbitrary numbers. "
					+ "You may change it later via menu 'administration / performance setting'");
			cpuIntensiveTaskConcurrency = 4;
			serverJobExecutorCpuQuota = 4000;
			serverJobExecutorMemoryQuota = 8000;
		}
	}

	@Editable(order=100, name="CPU 密集型任务并发", description="指定最大并发 CPU 密集型 "
			+ "任务, 比如 Git 仓库 pull/push, 存储库索引等.")
	public int getCpuIntensiveTaskConcurrency() {
		return cpuIntensiveTaskConcurrency;
	}

	public void setCpuIntensiveTaskConcurrency(int cpuIntensiveTaskConcurrency) {
		this.cpuIntensiveTaskConcurrency = cpuIntensiveTaskConcurrency;
	}

	@Editable(order=400, name="服务器作业执行器 CPU 配额", description="指定 CPU 配额以运行构建作业 "
			+ "在服务器上（通过服务器 docker/shell 执行器）. 这通常是 <i>(CPU cores)*1000</i>, 例如 "
			+ " <i>4000</i> 表示 4 个 CPU 内核. 在服务器上运行的所有构建作业的 CPU 要求 "
			+ "将受此配额限制")
	public int getServerJobExecutorCpuQuota() {
		return serverJobExecutorCpuQuota;
	}

	public void setServerJobExecutorCpuQuota(int serverJobExecutorCpuQuota) {
		this.serverJobExecutorCpuQuota = serverJobExecutorCpuQuota;
	}

	@Editable(order=500, name="服务器作业执行器内存配额", description="以兆字节指定内存配额 "
			+ "在服务器上运行构建作业（通过服务器 docker/shell 执行器）. 所有构建的内存要求 "
			+ "在服务器上运行的作业将受此配额限制")
	public int getServerJobExecutorMemoryQuota() {
		return serverJobExecutorMemoryQuota;
	}

	public void setServerJobExecutorMemoryQuota(int serverJobExecutorMemoryQuota) {
		this.serverJobExecutorMemoryQuota = serverJobExecutorMemoryQuota;
	}

	@Editable(order=600, name="最大 Git LFS 文件大小 (MB)", description="以兆字节指定最大 git LFS 文件大小")
	public int getMaxGitLFSFileSize() {
		return maxGitLFSFileSize;
	}

	public void setMaxGitLFSFileSize(int maxGitLFSFileSize) {
		this.maxGitLFSFileSize = maxGitLFSFileSize;
	}

	@Editable(order=700, name="最大上传文件大小 (MB)", description="指定上传文件的最大大小（以兆字节为单位）. "
			+ "这适用于通过 Web 界面上传到存储库的文件, 以及上传到markdown内容的文件 "
			+ "(发表评论等)")
	public int getMaxUploadFileSize() {
		return maxUploadFileSize;
	}

	public void setMaxUploadFileSize(int maxUploadFileSize) {
		this.maxUploadFileSize = maxUploadFileSize;
	}

	@Editable(order=800, description="在存储库中搜索代码时返回的最大条目数")
	public int getMaxCodeSearchEntries() {
		return maxCodeSearchEntries;
	}

	public void setMaxCodeSearchEntries(int maxCodeSearchEntries) {
		this.maxCodeSearchEntries = maxCodeSearchEntries;
	}

}
