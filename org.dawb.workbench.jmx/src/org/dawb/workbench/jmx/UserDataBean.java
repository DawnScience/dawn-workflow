package org.dawb.workbench.jmx;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserDataBean extends ActorBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6167207824021256404L;

	/**
	 * The data is either primitive array or IDataset
	 * It is the plotted data and the key is the trace name.
	 * 
	 * If tools will add traces, these traces will be included in this list.
	 */
	private Map<String,Serializable> data;
	
	/**
	 * Scalar data (passed in) plus anything that the tool used
	 * or the user edited, passed back.
	 */
	private Map<String,String>       scalar;
	
	/**
	 * The data extends ROIBase. The rois passed in will be created in the plotting system,
	 * the rois passed back will be those rois in the plotting system at the point where
	 * the cancel button is pressed. The Region name will be used as key. This would be the
	 * name propagated to the workflow normally.
	 */
	private Map<String,Serializable> rois;
	
	public void merge(UserDataBean with) {
		
		data   = mergeMap(data,   with.data);
		scalar = mergeStringMap(scalar, with.scalar);
		rois   = mergeMap(rois,   with.rois);
	}

	
	protected List<String> mergeList(List<String> col, List<String> with) {
		if (col==null) {
			return with;
		} else {
			if (with!=null) col.addAll(with);
		}
		return col;
	}

	protected Map<String, Serializable> mergeMap(Map<String, Serializable> col, Map<String, Serializable> with) {
		if (col==null) {
			return with;
		} else {
			if (with!=null) col.putAll(with);
		}
		return col;
	}


	protected Map<String, String> mergeStringMap(Map<String, String> col, Map<String, String> with) {
		if (col==null) {
			return with;
		} else {
			if (with!=null) col.putAll(with);
		}
		return col;
	}


	public Map<String, Serializable> getData() {
		return data;
	}

	public void setData(Map<String, Serializable> data) {
		this.data = data;
	}

	public Map<String, String> getScalar() {
		return scalar;
	}

	public void setScalar(Map<String, String> scalar) {
		this.scalar = scalar;
	}

	public Map<String, Serializable> getRois() {
		return rois;
	}

	public void setRois(Map<String, Serializable> rois) {
		this.rois = rois;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((rois == null) ? 0 : rois.hashCode());
		result = prime * result + ((scalar == null) ? 0 : scalar.hashCode());
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
		UserDataBean other = (UserDataBean) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
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
		return true;
	}

	public void addScalar(String name, String ads) {
		if (this.scalar==null) scalar = new LinkedHashMap<String, String>(7);
		scalar.put(name, ads);
	}
	public void addList(String name, Serializable ads) {
		if (this.data==null) data = new LinkedHashMap<String, Serializable>(7);
		data.put(name, ads);
	}
	public void addRoi(String name, Serializable roi) {
		if (rois==null) rois = new LinkedHashMap<String, Serializable>(4);
		rois.put(name, roi);
	}

	public boolean isEmpty() {
		return data==null&&rois==null&&scalar==null;
	}

	public int getDataSize() {
	    int size = 0;
	    if (data!=null)   size+=data.size();
	    if (scalar!=null) size+=scalar.size();
	    if (rois!=null)   size+=rois.size();
	    return size;
	}

}
