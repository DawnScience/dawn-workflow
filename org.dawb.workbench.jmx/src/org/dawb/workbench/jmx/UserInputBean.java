/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx;

import java.io.Serializable;
import java.util.Map;

public class UserInputBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -828017701067849684L;
	
	// These are the fields to edit, everything else is auto-generated.
	private String             partName;
	private boolean            isDialog;
	private String             configurationXML;
	private Map<String,String> scalarValues;
	private boolean            silent;
	
	
	public String getPartName() {
		return partName;
	}
	public void setPartName(String parentName) {
		this.partName = parentName;
	}
	public boolean isDialog() {
		return isDialog;
	}
	public void setDialog(boolean isDialog) {
		this.isDialog = isDialog;
	}
	public String getConfigurationXML() {
		return configurationXML;
	}
	public void setConfigurationXML(String configurationXML) {
		this.configurationXML = configurationXML;
	}
	public Map<String, String> getScalarValues() {
		return scalarValues;
	}
	public void setScalarValues(Map<String, String> scalarValues) {
		this.scalarValues = scalarValues;
	}
	public boolean isSilent() {
		return silent;
	}
	public void setSilent(boolean silent) {
		this.silent = silent;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((configurationXML == null) ? 0 : configurationXML.hashCode());
		result = prime * result + (isDialog ? 1231 : 1237);
		result = prime * result
				+ ((partName == null) ? 0 : partName.hashCode());
		result = prime * result
				+ ((scalarValues == null) ? 0 : scalarValues.hashCode());
		result = prime * result + (silent ? 1231 : 1237);
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
		UserInputBean other = (UserInputBean) obj;
		if (configurationXML == null) {
			if (other.configurationXML != null)
				return false;
		} else if (!configurationXML.equals(other.configurationXML))
			return false;
		if (isDialog != other.isDialog)
			return false;
		if (partName == null) {
			if (other.partName != null)
				return false;
		} else if (!partName.equals(other.partName))
			return false;
		if (scalarValues == null) {
			if (other.scalarValues != null)
				return false;
		} else if (!scalarValues.equals(other.scalarValues))
			return false;
		if (silent != other.silent)
			return false;
		return true;
	}
}
