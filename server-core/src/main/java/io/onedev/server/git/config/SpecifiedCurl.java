package io.onedev.server.git.config;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.OmitName;

@Editable(name="使用指定的curl", order=200)
public class SpecifiedCurl extends CurlConfig {

	private static final long serialVersionUID = 1L;
	
	private String curlPath;
	
	@Editable(name="curl Path", description="指定 curl 可执行文件的路径，例如: <tt>/usr/bin/curl</tt>")
	@OmitName
	@NotEmpty
	public String getCurlPath() {
		return curlPath;
	}

	public void setCurlPath(String curlPath) {
		this.curlPath = curlPath;
	}

	@Override
	public String getExecutable() {
		return curlPath;
	}

}
