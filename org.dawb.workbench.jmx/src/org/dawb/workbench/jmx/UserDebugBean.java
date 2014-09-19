/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawb.workbench.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * A bean 
 * 
 * @author Matthew Gerring
 *
 */
public class UserDebugBean extends UserDataBean{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6270121996201032744L;
	
	/**
	 * The data is either primitive array or IDataset
	 * It is the plotted data and the key is the trace name.
	 * 
	 * If tools will add traces, these traces will be included in this list.
	 */
	private Map<String,Serializable> outputData;
	
	/**
	 * Scalar data (passed in) plus anything that the tool used
	 * or the user edited, passed back.
	 */
	private Map<String,String>       outputScalar;
	
	/**
	 * The data extends ROIBase. The rois passed in will be created in the plotting system,
	 * the rois passed back will be those rois in the plotting system at the point where
	 * the cancel button is pressed. The Region name will be used as key. This would be the
	 * name propagated to the workflow normally.
	 */
	private Map<String,Serializable> outputRois;
	
	/**
	 * The port name that we are debugging on, if any.
	 * Can be null
	 */
	private String portName;

    public UserDebugBean() {
    	
    }
    public UserDebugBean(Map<String,String> vals) {
    	setScalar(vals);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((outputData == null) ? 0 : outputData.hashCode());
		result = prime * result
				+ ((outputRois == null) ? 0 : outputRois.hashCode());
		result = prime * result
				+ ((outputScalar == null) ? 0 : outputScalar.hashCode());
		result = prime * result
				+ ((portName == null) ? 0 : portName.hashCode());
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
		UserDebugBean other = (UserDebugBean) obj;
		if (outputData == null) {
			if (other.outputData != null)
				return false;
		} else if (!outputData.equals(other.outputData))
			return false;
		if (outputRois == null) {
			if (other.outputRois != null)
				return false;
		} else if (!outputRois.equals(other.outputRois))
			return false;
		if (outputScalar == null) {
			if (other.outputScalar != null)
				return false;
		} else if (!outputScalar.equals(other.outputScalar))
			return false;
		if (portName == null) {
			if (other.portName != null)
				return false;
		} else if (!portName.equals(other.portName))
			return false;
		return true;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}
	public Map<String, Serializable> getOutputData() {
		return outputData;
	}
	public void setOutputData(Map<String, Serializable> outputData) {
		this.outputData = outputData;
	}
	public Map<String, String> getOutputScalar() {
		return outputScalar;
	}
	public void setOutputScalar(Map<String, String> outputScalar) {
		this.outputScalar = outputScalar;
	}
	public Map<String, Serializable> getOutputRois() {
		return outputRois;
	}
	public void setOutputRois(Map<String, Serializable> outputRois) {
		this.outputRois = outputRois;
	}
	
	
	public int getDataSize() {
		int inputSize = super.getDataSize();
	    int size = 0;
	    if (outputData!=null)   size+=outputData.size();
	    if (outputScalar!=null) size+=outputScalar.size();
	    if (outputRois!=null)   size+=outputRois.size();
	    return Math.max(inputSize, size);
	}
	
	public List<Entry<String, ?>> getInputs() {
		Map<String, Serializable> scalarSerial = getScalar()!=null
                                               ? new LinkedHashMap<String, Serializable>(getScalar())
                                               : null;
		return getList(getData(), scalarSerial, getRois());
	}
	public List<Entry<String, ?>> getOutputs() {
		Map<String, Serializable> scalarSerial = getOutputScalar()!=null
				                               ? new LinkedHashMap<String, Serializable>(getOutputScalar())
				                               : null;
		return getList(getOutputData(), scalarSerial, getOutputRois());
	}
	
	private List<Entry<String, ?>> getList(Map<String, Serializable> data,
			                               Map<String, Serializable> scalar, 
                                           Map<String, Serializable> rois) {
		
		final List<Entry<String,?>> ret = new ArrayList<Map.Entry<String,?>>(7);
		if (data!=null)   addEntries(data.entrySet(),   ret);
		if (scalar!=null) addEntries(scalar.entrySet(), ret);
		if (rois!=null)   addEntries(rois.entrySet(),   ret);
		return ret;
	}
	private void addEntries(Set<Entry<String,  Serializable>> entrySet,
			                List<Entry<String, ?>> ret) {
		
		for (Entry<String, ?> entry : entrySet) {
			ret.add(entry);
		}
	}
	
	public void addOutputScalar(String name, String ads) {
		if (this.outputScalar==null) outputScalar = new LinkedHashMap<String, String>(7);
		outputScalar.put(name, ads);
	}


}
