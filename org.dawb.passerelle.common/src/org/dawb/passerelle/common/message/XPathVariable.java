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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dawb.passerelle.common.utils.XMLUtils;

public class XPathVariable extends Variable implements IVariable {

	protected String xPath;
	protected String rename;

	public XPathVariable(final String variableName,
                         final String saveString) {
		super(variableName, VARIABLE_TYPE.XML, "<xpath>?defined by "+getxPath(saveString)+"</xpath>");
		this.xPath  = getxPath(saveString);
		this.rename = getRename(saveString);
	}
	
	/**
	 * creates a string to save user defined data in the properties file.
	 * @return
	 */
	public String getSaveString() {
		if (rename==null||"".equals(rename)) return getxPath();
		return "@RENAME;'"+getRename()+"',"+getxPath();
	}
	
	public String getxPath() {
		return xPath;
	}

	public void setxPath(String xPath) {
		this.xPath = xPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((rename == null) ? 0 : rename.hashCode());
		result = prime * result + ((xPath == null) ? 0 : xPath.hashCode());
		return result;
	}
	public String getRename() {
		return rename;
	}

	public void setRename(String rename) {
		if (rename!=null) rename = rename.trim();
		this.rename = rename;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		XPathVariable other = (XPathVariable) obj;
		if (rename == null) {
			if (other.rename != null)
				return false;
		} else if (!rename.equals(other.rename))
			return false;
		if (xPath == null) {
			if (other.xPath != null)
				return false;
		} else if (!xPath.equals(other.xPath))
			return false;
		return true;
	}

	private static final Pattern XPATH_PATTERN = Pattern.compile("\\@RENAME\\;\\'([a-zA-Z0-9_ ]+)\\'\\,(.+)");
	
	private static String getxPath(final String saveString) {
	    final Matcher matcher = XPATH_PATTERN.matcher(saveString);
	    if (matcher.matches()) return matcher.group(2);
	    return saveString;
	}
	
	private static String getRename(final String saveString) {
	    final Matcher matcher = XPATH_PATTERN.matcher(saveString);
	    if (matcher.matches()) return matcher.group(1);
	    return null;
	}
	
	/**
	 * Use to get the encoded xpaths out of the properties files.
	 * @param props
	 * @return
	 */
	public static Map<String,String> getXPaths(final Map/**<String,String>**/ props) {
		if (props==null) return null;
		final Map<String,String> ret = new HashMap<String,String>(props.size());
		for (Object key : props.keySet()) {
			ret.put((String)key, getxPath((String)props.get(key)));
		}
		return ret;
	}

	public static Map<String, String> getRenames(Map/**<String,String>**/ props) {
		if (props==null) return null;
		final Map<String,String> ret = new HashMap<String,String>(props.size());
		for (Object key : props.keySet()) {
			final String rename = getRename((String)props.get(key));
			if (rename==null||"".equals(rename)) continue;
			ret.put((String)key, rename);
		}
		return ret;
	}
	

	@Override
	public String getErrorMessage() {
		
		if (super.getErrorMessage()!=null) return super.getErrorMessage();
		try {
			if (getxPath()!=null && !"".equals(getxPath().trim())) {
				XMLUtils.isLegalXPath(getxPath());
			}
		} catch (Exception ne) {
			if (ne.getMessage()!=null) return ne.getMessage();
			if (ne.getCause().getMessage()!=null) return ne.getCause().getMessage();
			return "Problem parsing xpath '"+getxPath()+"'";
		}
		if ( rename!=null && !"".equals(rename.trim()) && !rename.matches("[a-zA-Z0-9_]+") ) {
			return "The rename tag must be alphnumeric and not contain spaces.";
		}
		return null;
	}

}
