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


/**
 * Vanilla class to act as a bean for a single value.
 * Not intended for external use.
 */
public class StringValueBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5381376224255062676L;
	
	private String textValue;

	public StringValueBean() {
		
	}
	
	public StringValueBean(String value) {
		this.setTextValue(value);
	}

	/**
	 * @return Returns the value.
	 */
	public String getTextValue() {
		return textValue;
	}

	/**
	 * @param value The value to set.
	 */
	public void setTextValue(String value) {
		this.textValue = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((textValue == null) ? 0 : textValue.hashCode());
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
		StringValueBean other = (StringValueBean) obj;
		if (textValue == null) {
			if (other.textValue != null)
				return false;
		} else if (!textValue.equals(other.textValue))
			return false;
		return true;
	}
	
}
