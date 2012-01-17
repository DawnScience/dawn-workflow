/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ui.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FieldContainer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2809239217814025633L;
	
	
	private List<FieldBean> fields;
	private String          customLabel;
	
	public FieldContainer() {
		fields = new ArrayList<FieldBean>();
	}
	
	public void clear() {
		if (fields!=null) fields.clear();
	}

	public List<FieldBean> getFields() {
		return fields;
	}

	public void setFields(List<FieldBean> expressions) {
		this.fields = expressions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((customLabel == null) ? 0 : customLabel.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		FieldContainer other = (FieldContainer) obj;
		if (customLabel == null) {
			if (other.customLabel != null)
				return false;
		} else if (!customLabel.equals(other.customLabel))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

	public void addField(FieldBean expressionBean) {
		if (fields==null) fields =  new ArrayList<FieldBean>(7);
		fields.add(expressionBean);
	}

	public int size() {
		return fields.size();
	}

	public boolean isEmpty() {
		return fields==null||size()<1;
	}
	
	public String toString() {
		
		if (isEmpty()) return "No fields set, please click and create user fields...";
		
		final StringBuilder buf = new StringBuilder();
		for (FieldBean fb : fields) {
			buf.append("'");
			buf.append(fb.getVariableName());
			buf.append("'");
			
			if (fb.getClassString()!=null) {
				buf.append("(");
				buf.append(fb.getClassString());
				buf.append(")");
			}
			buf.append("  ");
		}
		
		return buf.toString();
	}

	public Collection<String> getNames() {
		
		if (isEmpty()) return Collections.emptyList();
		final Collection<String> ret = new ArrayList<String>(fields.size());
		for (FieldBean fb : fields) ret.add(fb.getVariableName());
		
		return ret;
	}

	/**
	 * Does a loop at the moment but a short one.
	 * @param name
	 * @return
	 */
	public FieldBean getBean(String name) {
		
		if (name==null||"".equals(name)) return null;
		if (isEmpty()) return null;
		for (FieldBean fb : fields) if (fb.getVariableName().equals(name)) return fb;
		return null;
	}

	public boolean containsBean(String valueName) {
		return getNames().contains(valueName);
	}

	public String getCustomLabel() {
		return customLabel;
	}

	public void setCustomLabel(String customLabel) {
		this.customLabel = customLabel;
	}
}
