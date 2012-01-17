/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.flow;

import java.util.ArrayList;
import java.util.List;

public class ExpressionContainer {

	private List<ExpressionBean> expressions;
	
	public ExpressionContainer() {
		expressions = new ArrayList<ExpressionBean>();
	}
	
	public void clear() {
		if (expressions!=null) expressions.clear();
	}

	public List<ExpressionBean> getExpressions() {
		return expressions;
	}

	public void setExpressions(List<ExpressionBean> expressions) {
		this.expressions = expressions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((expressions == null) ? 0 : expressions.hashCode());
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
		ExpressionContainer other = (ExpressionContainer) obj;
		if (expressions == null) {
			if (other.expressions != null)
				return false;
		} else if (!expressions.equals(other.expressions))
			return false;
		return true;
	}

	public boolean containsActor(String name) {
		return getBean(name)!=null;
	}

	public void addExpression(ExpressionBean expressionBean) {
		if (expressions==null) expressions =  new ArrayList<ExpressionBean>(7);
		expressions.add(expressionBean);
	}

	public ExpressionBean getBean(String name) {
		if (name==null)        return null;
		if (expressions==null) return null;
		for (ExpressionBean b : expressions) {
			if (name.equals(b.getActorName())) return b;
		}
		return null;
	}
	
	/**
	 * Constructs user readable version of bean
	 */
	public String toString() {
		if (expressions==null||expressions.isEmpty()) return "true";
		final StringBuilder buf = new StringBuilder();
		for (ExpressionBean b : expressions) {
			buf.append("if(");
			buf.append(b.getExpression());
			buf.append(")");
			buf.append("{");
			buf.append(b.getActorName());
			buf.append("}   ");
		}
		return buf.toString();
	}
}
