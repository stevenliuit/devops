package io.onedev.server.model.support.administration;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class BrandingSetting implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name = "代码管理系统";
	
	@Editable(order=100, description="指定将显示在屏幕左上方的品牌名称")
	@NotEmpty
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

}
