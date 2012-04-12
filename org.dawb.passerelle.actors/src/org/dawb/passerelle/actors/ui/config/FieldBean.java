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
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

/**
 * 
 @See uk.ac.gda.richbeans.beans.package-info.java
 */
public class FieldBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7494988708038548535L;
	/**
	 * A class extending uk.ac.gda.richbeans.components.FieldComposite most likely
	 */
	private String  variableName;
	private String  uiClass;
	private String  uiLabel;
	private Number  upperBound;
	private Number  lowerBound;
	private String  unit;
	private Object  defaultValue;
	private Integer textLimit;
	private boolean isFolder;
	private boolean isPassword;
	private String  fileFilterLabel;
	private String  extensions;
	private List<StringValueBean> textChoices;
	
	public FieldBean() {
		this.variableName = "x";
		this.uiClass = "uk.ac.gda.richbeans.components.wrappers.TextWrapper";
		uiLabel      = null;
		upperBound   = 1000;
		lowerBound   = 0;
		unit         = null;
		textLimit    = 0;
		isFolder     = true;
		fileFilterLabel="All";
		extensions   = "*";
		textChoices  = new ArrayList<StringValueBean>(7);
	}


	/**
	 * Must implement clear() method on beans being used with BeanUI.
	 */
	public void clear() {
		textChoices.clear();
	}
 

	/**
	 *
	 */
	@Override
	public String toString() {
		try {
			return BeanUtils.describe(this).toString();
		} catch (Exception e) {
			return e.getMessage();
		}
	}


	public String getUiClass() {
		return uiClass;
	}



	public void setUiClass(String uiClass) {
		this.uiClass = uiClass;
	}


	public Number getUpperBound() {
		return upperBound;
	}


	public void setUpperBound(Number upperBound) {
		this.upperBound = upperBound;
	}


	public Number getLowerBound() {
		return lowerBound;
	}


	public void setLowerBound(Number lowerBound) {
		this.lowerBound = lowerBound;
	}


	public Object getDefaultValue() {
		return defaultValue;
	}


	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}


	public String getExtensions() {
		return extensions;
	}


	public void setExtensions(String extensions) {
		this.extensions = extensions;
	}


	public String getFileFilterLabel() {
		return fileFilterLabel;
	}



	public void setFileFilterLabel(String fileFilterName) {
		this.fileFilterLabel = fileFilterName;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result
				+ ((extensions == null) ? 0 : extensions.hashCode());
		result = prime * result
				+ ((fileFilterLabel == null) ? 0 : fileFilterLabel.hashCode());
		result = prime * result + (isFolder ? 1231 : 1237);
		result = prime * result + (isPassword ? 1231 : 1237);
		result = prime * result
				+ ((lowerBound == null) ? 0 : lowerBound.hashCode());
		result = prime * result
				+ ((textChoices == null) ? 0 : textChoices.hashCode());
		result = prime * result
				+ ((textLimit == null) ? 0 : textLimit.hashCode());
		result = prime * result + ((uiClass == null) ? 0 : uiClass.hashCode());
		result = prime * result + ((uiLabel == null) ? 0 : uiLabel.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		result = prime * result
				+ ((upperBound == null) ? 0 : upperBound.hashCode());
		result = prime * result
				+ ((variableName == null) ? 0 : variableName.hashCode());
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
		FieldBean other = (FieldBean) obj;
		if (defaultValue == null) {
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (extensions == null) {
			if (other.extensions != null)
				return false;
		} else if (!extensions.equals(other.extensions))
			return false;
		if (fileFilterLabel == null) {
			if (other.fileFilterLabel != null)
				return false;
		} else if (!fileFilterLabel.equals(other.fileFilterLabel))
			return false;
		if (isFolder != other.isFolder)
			return false;
		if (isPassword != other.isPassword)
			return false;
		if (lowerBound == null) {
			if (other.lowerBound != null)
				return false;
		} else if (!lowerBound.equals(other.lowerBound))
			return false;
		if (textChoices == null) {
			if (other.textChoices != null)
				return false;
		} else if (!textChoices.equals(other.textChoices))
			return false;
		if (textLimit == null) {
			if (other.textLimit != null)
				return false;
		} else if (!textLimit.equals(other.textLimit))
			return false;
		if (uiClass == null) {
			if (other.uiClass != null)
				return false;
		} else if (!uiClass.equals(other.uiClass))
			return false;
		if (uiLabel == null) {
			if (other.uiLabel != null)
				return false;
		} else if (!uiLabel.equals(other.uiLabel))
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		if (upperBound == null) {
			if (other.upperBound != null)
				return false;
		} else if (!upperBound.equals(other.upperBound))
			return false;
		if (variableName == null) {
			if (other.variableName != null)
				return false;
		} else if (!variableName.equals(other.variableName))
			return false;
		return true;
	}
	
	public String getErrorMessage() {
		return null;
	}


	public Integer getTextLimit() {
		return textLimit;
	}


	public void setTextLimit(Integer textLimit) {
		this.textLimit = textLimit;
	}


	public String getUnit() {
		return unit;
	}


	public void setUnit(String unit) {
		this.unit = unit;
	}


	public boolean isFolder() {
		return isFolder;
	}


	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}


	public String getUiLabel() {
		return uiLabel;
	}


	public void setUiLabel(String uiLabel) {
		this.uiLabel = uiLabel;
	}


	public String getUserString() {
		final StringBuffer buf = new StringBuffer();
		buf.append(getClassString());
		buf.append("  ");
		if (getUiLabel()!=null) {
			buf.append("'");
			buf.append(getUiLabel());
			buf.append("' ");
		} 
		
		if (getDefaultValue()!=null) {
			buf.append("'Default Value=");
			buf.append(getDefaultValue());
			if (getUnit()!=null) {
				buf.append(" [");	
				buf.append(unit);	
				buf.append("]");	
			}
			buf.append("'  ");
		} 
		
		return buf.toString();
	}

	public String getClassString() {
		if (uiClass.endsWith(".TextWrapper"))    return "Text";
		if (uiClass.endsWith(".ComboWrapper"))   return "Combo";
		if (uiClass.endsWith(".StandardBox"))    return "Real";
		if (uiClass.endsWith(".SpinnerWrapper")) return "Integer";
		if (uiClass.endsWith(".FileBox"))        return "File";
		return uiClass;
	}


	public List<StringValueBean> getTextChoices() {
		return textChoices;
	}


	public void setTextChoices(List<StringValueBean> textChoices) {
		this.textChoices = textChoices;
	}


	public List<String> getTextChoicesAsStrings() {
		final List<StringValueBean> beans = getTextChoices();
        if (beans==null || beans.isEmpty()) return null;
        
        final List<String> ret = new ArrayList<String>(beans.size());
        for (StringValueBean bean : beans)ret.add(bean.getTextValue());
		
        return ret;
	}


	public String getVariableName() {
		return variableName;
	}


	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}


	public boolean isPassword() {
		return isPassword;
	}


	public void setPassword(boolean isPassword) {
		this.isPassword = isPassword;
	}

}
