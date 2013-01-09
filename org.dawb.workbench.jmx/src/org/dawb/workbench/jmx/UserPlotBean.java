package org.dawb.workbench.jmx;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Bean holds data sent to plot something and also much that the user did 
 * is returned to the actor calling the createPlotInput(...) method of
 * IRemoteWorkbench.
 * 
 * @author fcp94556
 *
 */
public class UserPlotBean extends ActorBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3845362626954632932L;
	
	/**
	 * True if the user used this bean to plot something and then pressed ok
	 * to continue.
	 */
	private boolean userPlottedSomething = false;


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
	
	/**
	 * The list of the axes in the order x,y,z. Made be size one, two or three.
	 * If size =1 we are 1D and the x-axis is the name defined
	 * If size =2 we are 2D and the a x and y labels are defined.
	 * If size =3 we are 2D and the a x, y and z labels are defined.
	 * 
	 * These names must exist in the data and be 1D abstract datasets of the correct size.
	 * (dim1 of image = y   and   dim2 of image = x).
	 */
	private List<String> axesNames;
	
	/**
	 * If a tool has been run its data may be provided here.
	 * If a tool should be selected by default, its id is here.
	 */
	private Serializable toolData;
	
	/**
	 * If the user used a tool, this is the id of the tool which they last used.
	 * If other tools were used, they may have set data in the data or the regions
	 * or their data may have been lost.
	 */
	private String toolId;

	public Serializable getToolData() {
		return toolData;
	}

	public void setToolData(Serializable toolData) {
		this.toolData = toolData;
	}

	public String getToolId() {
		return toolId;
	}

	public void setToolId(String toolId) {
		this.toolId = toolId;
	}

	public Map<String, Serializable> getData() {
		return data;
	}

	public void setData(Map<String, Serializable> list) {
		this.data = list;
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
		result = prime * result
				+ ((axesNames == null) ? 0 : axesNames.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((rois == null) ? 0 : rois.hashCode());
		result = prime * result + ((scalar == null) ? 0 : scalar.hashCode());
		result = prime * result
				+ ((toolData == null) ? 0 : toolData.hashCode());
		result = prime * result + ((toolId == null) ? 0 : toolId.hashCode());
		result = prime * result + (userPlottedSomething ? 1231 : 1237);
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
		UserPlotBean other = (UserPlotBean) obj;
		if (axesNames == null) {
			if (other.axesNames != null)
				return false;
		} else if (!axesNames.equals(other.axesNames))
			return false;
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
		if (toolData == null) {
			if (other.toolData != null)
				return false;
		} else if (!toolData.equals(other.toolData))
			return false;
		if (toolId == null) {
			if (other.toolId != null)
				return false;
		} else if (!toolId.equals(other.toolId))
			return false;
		if (userPlottedSomething != other.userPlottedSomething)
			return false;
		return true;
	}

	public boolean isUserPlottedSomething() {
		return userPlottedSomething;
	}

	public void setUserPlottedSomething(boolean userPlottedSomething) {
		this.userPlottedSomething = userPlottedSomething;
	}

	public List<String> getAxesNames() {
		return axesNames;
	}

	public void setAxesNames(List<String> axesNames) {
		this.axesNames = axesNames;
	}

}
