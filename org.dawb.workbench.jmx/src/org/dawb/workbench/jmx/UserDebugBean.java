package org.dawb.workbench.jmx;

import java.io.Serializable;

/**
 * A bean 
 * 
 * @author fcp94556
 *
 */
public class UserDebugBean extends ActorBean{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6270121996201032744L;

	public enum DEBUG_TYPE {
		BEFORE_ACTOR, AFTER_ACTOR, BOTH;
	}

	/**
	 * The component which contains the information we would
	 * like to debug in value view. This is normally cast to a
	 * DataMessageComponent.
	 */
	private Serializable dataMessageComponent;
	
	/**
	 * The break point position.
	 */
	private DEBUG_TYPE debugType;

	public Serializable getDataMessageComponent() {
		return dataMessageComponent;
	}

	public void setDataMessageComponent(Serializable dataMessageComponent) {
		this.dataMessageComponent = dataMessageComponent;
	}

	public DEBUG_TYPE getDebugType() {
		return debugType;
	}

	public void setDebugType(DEBUG_TYPE debugType) {
		this.debugType = debugType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((dataMessageComponent == null) ? 0 : dataMessageComponent
						.hashCode());
		result = prime * result
				+ ((debugType == null) ? 0 : debugType.hashCode());
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
		if (dataMessageComponent == null) {
			if (other.dataMessageComponent != null)
				return false;
		} else if (!dataMessageComponent.equals(other.dataMessageComponent))
			return false;
		if (debugType != other.debugType)
			return false;
		return true;
	}
	
}
