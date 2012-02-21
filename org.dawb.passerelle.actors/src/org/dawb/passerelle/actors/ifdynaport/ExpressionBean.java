/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ifdynaport;

public class ExpressionBean {

	private String outputPortName = "output";
	private String expression = "true";
	
	public ExpressionBean() {}
	public ExpressionBean(String name, String expr) {
		setOutputPortName(name);
		setExpression(expr);
	}
	public String getOutputPortName() {
		return outputPortName;
	}
	public void setOutputPortName(String outputPortName) {
		this.outputPortName = outputPortName;
	}
	public String getExpression() {
		return expression;
	}
	public void setExpression(String expression) {
		this.expression = expression;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((outputPortName == null) ? 0 : outputPortName.hashCode());
		result = prime * result
				+ ((expression == null) ? 0 : expression.hashCode());
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
		ExpressionBean other = (ExpressionBean) obj;
		if (outputPortName == null) {
			if (other.outputPortName != null)
				return false;
		} else if (!outputPortName.equals(other.outputPortName))
			return false;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		return true;
	}
}
