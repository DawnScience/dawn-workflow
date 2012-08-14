package org.dawb.passerelle.common.parameter;

import org.eclipse.core.variables.IValueVariable;

public class UtilValueVariable implements IValueVariable {

	private String name;
	private String description;
	private String value;
	
	public UtilValueVariable(String name, String description, String value) {
		this.name = name;
		this.description = description;
		this.value = value;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean isContributed() {
		return false;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
		
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
}
