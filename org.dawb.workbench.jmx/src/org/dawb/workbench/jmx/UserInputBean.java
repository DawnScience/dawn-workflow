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


public class UserInputBean extends UserDataBean {
	/**
	 * 
	 */
	private static final long serialVersionUID = -828017701067849684L;
	
	// These are the fields to edit, everything else is auto-generated.
	private String             configurationXML;
	
	public String getConfigurationXML() {
		return configurationXML;
	}
	public void setConfigurationXML(String configurationXML) {
		this.configurationXML = configurationXML;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((configurationXML == null) ? 0 : configurationXML.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserInputBean other = (UserInputBean) obj;
		if (configurationXML == null) {
			if (other.configurationXML != null)
				return false;
		} else if (!configurationXML.equals(other.configurationXML))
			return false;
		return true;
	}
}
