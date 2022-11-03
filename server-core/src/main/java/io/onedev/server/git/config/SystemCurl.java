package io.onedev.server.git.config;

import io.onedev.server.web.editable.annotation.Editable;

@Editable(name="在系统路径中使用 curl", order=100)
public class SystemCurl extends CurlConfig {

	private static final long serialVersionUID = 1L;

	@Override
	public String getExecutable() {
		return "curl";
	}

}
