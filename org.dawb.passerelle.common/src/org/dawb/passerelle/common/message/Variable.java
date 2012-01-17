/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.message;

public class Variable implements IVariable {

	private String        variableName;
	private Object        exampleValue;
	private VARIABLE_TYPE variableType;
	private Class<?>      variableClass;
	
	public Variable(final String        variableName,
			        final VARIABLE_TYPE variableType,
			        final Object        exampleValue) {
		this(variableName,variableType,exampleValue,null);
	}
	
	public Variable(final String        variableName, 
			        final VARIABLE_TYPE variableType,
	                final Object        exampleValue, 
	                final Class<?>      clazz) {
		setVariableName(variableName);
		setVariableType(variableType);
		setExampleValue(exampleValue);
		setVariableClass(clazz);
	}

	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	public VARIABLE_TYPE getVariableType() {
		return variableType;
	}
	public void setVariableType(VARIABLE_TYPE variableType) {
		this.variableType = variableType;
	}
	public Class<?> getVariableClass() {
		return variableClass;
	}
	public void setVariableClass(Class<?> caraiableClass) {
		this.variableClass = caraiableClass;
	}
	public String toString() {
		return variableName;
	}
	public Object getExampleValue() {
		return exampleValue;
	}

	public void setExampleValue(Object exampleValue) {
		this.exampleValue = exampleValue;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((exampleValue == null) ? 0 : exampleValue.hashCode());
		result = prime * result
				+ ((variableName == null) ? 0 : variableName.hashCode());
		result = prime * result
				+ ((variableType == null) ? 0 : variableType.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Variable other = (Variable) obj;
		if (exampleValue == null) {
			if (other.exampleValue != null)
				return false;
		} else if (!exampleValue.equals(other.exampleValue))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		if (variableType != other.variableType)
			return false;
		return true;
	}

	@Override
	public String getErrorMessage() {
		if ( variableName!=null && !variableName.matches("[a-zA-Z0-9_ \\$\\{\\}]+") ) {
			return "Variable name '"+variableName+"' should be alpha-numeric values or underscore only.";
		}
		return null;
	}

}
