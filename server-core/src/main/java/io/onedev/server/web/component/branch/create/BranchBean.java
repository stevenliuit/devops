package io.onedev.server.web.component.branch.create;

import java.io.Serializable;

import javax.validation.ConstraintValidatorContext;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import javax.validation.constraints.NotEmpty;

import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.OmitName;

@Editable
@ClassValidating
public class BranchBean implements Validatable, Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	
	@Editable(order=100, name="分支名称")
	@NotEmpty
	@OmitName
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		if (!Repository.isValidRefName(Constants.R_HEADS + getName())) {
            context.buildConstraintViolationWithTemplate("无效的分支名称")
		            .addPropertyNode("name").addConstraintViolation()
		            .disableDefaultConstraintViolation();
            return false;
		} else {
			return true;
		}
	}
	
}
