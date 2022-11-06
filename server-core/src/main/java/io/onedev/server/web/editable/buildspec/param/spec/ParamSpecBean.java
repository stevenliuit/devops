package io.onedev.server.web.editable.buildspec.param.spec;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.onedev.server.buildspec.param.spec.ParamSpec;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class ParamSpecBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private ParamSpec paramSpec;

	// change Named("paramSpec") also if change name of this property 
	@Editable(name="Type", order=100)
	@NotNull(message="不能为空")
	public ParamSpec getParamSpec() {
		return paramSpec;
	}

	public void setParamSpec(ParamSpec paramSpec) {
		this.paramSpec = paramSpec;
	}

}
