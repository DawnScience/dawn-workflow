package org.dawb.workbench.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
public class UserPlotBean extends UserDataBean {

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
	 * Functions extending AFuction usually.
	 */
	private Map<String,Serializable> functions;

	
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
	 * The name(s) of the data to use, may be null.
	 */
	private List<String> dataNames;

	/**
	 * If a tool has been run its data may be provided here.
	 * If a tool should be selected by default, its id is here.
	 */
	private Serializable toolData;
	
	/**
	 * A description for explaining to the user how they are supposed to 
	 * use the plot. May be null
	 */
	private String description;
	
	/**
	 * If the user used a tool, this is the id of the tool which they last used.
	 * If other tools were used, they may have set data in the data or the regions
	 * or their data may have been lost.
	 */
	private String toolId;
	
	/**
	 * The user may choose to run a tool over the plot automatically
	 * without prompting them again with a dialog. If set, the last tool,
	 * in its last configuration will be run again.
	 */
	private boolean isAutomaticallyApply;
	
	public void merge(UserPlotBean with) {
		super.merge(with);
		axesNames = mergeList(axesNames, with.axesNames);		
		dataNames = mergeList(dataNames, with.dataNames);
		
		functions = mergeMap(functions, with.functions);
	}


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


	public UserPlotBean clone() {
		UserPlotBean ret = new UserPlotBean();
		
		ret.setData  (getData()!=null  ? new LinkedHashMap<String, Serializable>(getData()): null);
		ret.setScalar(getScalar()!=null? new LinkedHashMap<String, String>(getScalar())    : null);		
		ret.setRois  (getRois()!=null  ? new LinkedHashMap<String, Serializable>(getRois()): null);
		
		ret.setAxesNames(getAxesNames()!=null?   new ArrayList<String>(getAxesNames()): null);
		ret.setDescription(getDescription());
		ret.setToolData(getToolData());
		
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((axesNames == null) ? 0 : axesNames.hashCode());
		result = prime * result
				+ ((dataNames == null) ? 0 : dataNames.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((functions == null) ? 0 : functions.hashCode());
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
		if (dataNames == null) {
			if (other.dataNames != null)
				return false;
		} else if (!dataNames.equals(other.dataNames))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (functions == null) {
			if (other.functions != null)
				return false;
		} else if (!functions.equals(other.functions))
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void clearAxisNames() {
		if (axesNames!=null) axesNames.clear();
	}
	public void addAxisName(String name) {
		if (axesNames==null) axesNames = new ArrayList<String>(3);
		axesNames.add(name);
	}

	public boolean isEmpty() {
		return super.isEmpty()&&description==null&&toolData==null&&toolId==null;
	}

	public List<String> getDataNames() {
		return dataNames;
	}

	public void setDataNames(List<String> dataNames) {
		this.dataNames = dataNames;
	}

	public Map<String, Serializable> getFunctions() {
		return functions;
	}

	public void setFunctions(Map<String, Serializable> functions) {
		this.functions = functions;
	}


	public boolean isAutomaticallyApply() {
		return isAutomaticallyApply;
	}


	public void setAutomaticallyApply(boolean isShowDialog) {
		this.isAutomaticallyApply = isShowDialog;
	}

}
