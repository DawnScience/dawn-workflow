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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.gda.util.map.MapUtils;

/**
 * This class is similar to a DataHolder in the scisoft diamond
 * plugins.
 * 
 * It is simpler and less subject to use else where being the main thing
 * which is passed around in the workflow system.
 *
 * It contains data and provenance, meta data.
 * 
 * @author gerring
 *
 */
public class DataMessageComponent {

	public enum VALUE_TYPE {
		/**
		 * Adds one string value to another if two are
		 * in one list
		 */
		ADDITIVE_STRING, 
		
		/**
		 * Overwrites the value if it exists already.
		 */
		OVERWRITE_STRING
	}
	
	/**
	 * The data is either primitive array or IDataset
	 */
	private Map<String,Serializable> list;
	/**
	 * What we did to the data in the pipeline
	 */
	private Map<String,String>   scalar;
	/**
	 * The data extends ROIBase
	 */
	private Map<String,Serializable> rois;
	
	/**
	 * A set of source meta data which may be altered
	 * to add more information if needed.
	 */
	private IMetaData            meta;
	
	/**
	 * 
	 */
	private Map<String,VALUE_TYPE>   valueTypes;
	
	/**
	 * Indicates if the message was generated during an error stream.
	 * @return
	 */
	private boolean error = false;


	public Map<String, Serializable> getList() {
		return list;
	}
	
	public Object getList(final String key) {
		if (list!=null) return list.get(key);
		return null;
	}

	public void setList(Map<String, Serializable> data) {
		this.list = data;
	}

	public IMetaData getMeta() {
		return meta;
	}

	public void setMeta(IMetaData metaData) {
		this.meta = metaData;
	}
	
	public void setList(final IDataset set) {
		if (list==null) list = new LinkedHashMap<String,Serializable>(1);
		String name = set.getName();
		if (name==null||"".equals(name)) name = "Unknown";
		MapUtils.putUnique(list, name, set);
	}
	
    /**
     * Renames a list in the DataMessageComponent
     * @param existing
     * @param name
     * @return true if the name replaced was already there.
     */
	public boolean renameList(String existing, String name) {
		if (list==null) return false;
		
		Map<String,Serializable> map = new LinkedHashMap<String,Serializable>(1);
		map.putAll(list);
		final Serializable set = map.remove(existing);
		if (set instanceof IDataset) {
			((IDataset)set).setName(name);
		}
	
		boolean replaced = map.put(name, set) != null;
		this.list = map;
		return replaced;
	}

	
	public void addScalar(final Map<String,String> f) {
		addScalar(f, true);
	}

	public void addScalar(final Map<String, String> toAdd, boolean overwrite) {
		
		if (toAdd==null) return;
		
		if (scalar==null) scalar = new Hashtable<String,String>(toAdd.size());
		if (overwrite) {
			for (String key : toAdd.keySet()) {
				if (toAdd.get(key)!=null) {
					scalar.remove(key);
					scalar.put(key, toAdd.get(key));
				}
			}
		    
		} else {
			final Map<String, String> copy = new HashMap<String,String>(toAdd);
			copy.keySet().removeAll(scalar.keySet());
			for (String key : copy.keySet()) {
				if (copy.get(key)!=null) scalar.put(key, copy.get(key));
			}
		}
	}
	
	public void putScalar(final String key, final String value) {
		if (value == null && scalar!=null) {
			scalar.remove(key);
			return;
		}
		if (scalar==null) scalar = new Hashtable<String,String>(1);
		scalar.put(key,value);
	}
	
	public Map<String,String> getScalar() {
		return scalar;
	}
	
	public String getScalar(final String key) {
		if (scalar==null) return null;
		return scalar.get(key);
	}

	
	@Override
    public String toString() {
		StringBuilder buf = new StringBuilder("");
		if (list!=null&&!list.isEmpty()) {
			// Sorts the key set of the list
			Set<String> keySetList = new TreeSet<String>(list.keySet());
			for (String name: keySetList) {
				if (list.get(name)==null) continue;
				buf.append(this.formatString(name, list.get(name).toString()));
				buf.append("\n");
			}
		}
		if (scalar!=null&&!scalar.isEmpty()) {
			// Sorts the key set of the scalars
			Set<String> keySetList = new TreeSet<String>(scalar.keySet());
			for (String name: keySetList) {
				buf.append(this.formatString(name, scalar.get(name).toString()));
				buf.append("\n");
			}
		}
		if (meta!=null) {
			buf.append(meta.toString());
		}
		if ("".equals(buf.toString())) return super.toString();
		return buf.toString();
	}

	/**
	 * This private method format a given name and value to:
	 * 'name          : value [eventually trunkated]'
	 * @param name
	 * @param value
	 * @return
	 */
	private String formatString(String name, String value) {
		int maxStringLength = 60;
		int tabStop = 20;
		String objectName = name;
		String objectString = value.replaceAll("\n", "");
		if (objectName.length() < tabStop) objectName = String.format("%1$-" + tabStop + "s", objectName);
		if (objectString.length() > maxStringLength) objectString = objectString.substring(0, maxStringLength) + "...";
		return objectName + " : " + objectString;				
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (error ? 1231 : 1237);
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		result = prime * result + ((meta == null) ? 0 : meta.hashCode());
		result = prime * result + ((rois == null) ? 0 : rois.hashCode());
		result = prime * result + ((scalar == null) ? 0 : scalar.hashCode());
		result = prime * result
				+ ((valueTypes == null) ? 0 : valueTypes.hashCode());
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
		DataMessageComponent other = (DataMessageComponent) obj;
		if (error != other.error)
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (meta == null) {
			if (other.meta != null)
				return false;
		} else if (!meta.equals(other.meta))
			return false;
		if (rois == null) {
			if (other.rois != null)
				return false;
		} else if (!rois.equals(other.rois))
			return false;
		if (scalar == null) {
			if (other.scalar != null)
				return false;
		} else if (!scalar.equals(other.scalar))
			return false;
		if (valueTypes == null) {
			if (other.valueTypes != null)
				return false;
		} else if (!valueTypes.equals(other.valueTypes))
			return false;
		return true;
	}

	public VALUE_TYPE getValueType(String key) {
		if (valueTypes==null) return VALUE_TYPE.OVERWRITE_STRING;
		return valueTypes.get(key);
	}
	
	public void setValueType(final String key, final VALUE_TYPE type) {		
		if (valueTypes==null) valueTypes = new Hashtable<String,VALUE_TYPE>(7);
		valueTypes.put(key, type);
	}

	public void add(DataMessageComponent a) {
		
		if (a.list!=null) {
			if (list==null) list = new LinkedHashMap<String, Serializable>(a.list.size());
		    list.putAll(a.list);
		}
		
		if (a.scalar!=null) {
			if (scalar==null) scalar = new Hashtable<String, String>(a.scalar.size());
			scalar.putAll(a.scalar);
		}
		if (a.meta!=null)  meta = a.meta;
		
		if (a.valueTypes!=null) {
			if (valueTypes==null) valueTypes = new Hashtable<String, VALUE_TYPE>(a.valueTypes.size());
			valueTypes.putAll(a.valueTypes);
		}
	}

	public boolean isScalarOnly() {
		return (list==null || list.isEmpty()) && scalar!=null && scalar.size()>0;
	}

	public void addList(String name, AbstractDataset a) {
		if (list==null) list = new LinkedHashMap<String,Serializable>(1);
		list.put(name, a);
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}
	
	
	// ROI Methods
	public void addROI(String name, ROIBase roi) {
		if (rois == null) rois = new LinkedHashMap<String,Serializable>(1);
		rois.put(name, roi);
	}
	
	public ROIBase getROI(String name) {
		if (rois == null) return null;
		return (ROIBase) rois.get(name);
	}
	
	public void clearROI() {
		rois.clear();
		rois = null;
	}
	public Map<String, Serializable> getRois() {
		return rois;
	}

}
