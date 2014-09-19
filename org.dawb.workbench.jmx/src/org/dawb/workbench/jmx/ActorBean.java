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

/**
 * A bean carrying information about an actor.
 * 
 * @author Matthew Gerring
 *
 */
public class ActorBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -777105045012590030L;
	
	private String             actorName;
	private String             partName;
	private boolean            silent;
	private boolean            isDialog;

	public boolean isSilent() {
		return silent;
	}
	public void setSilent(boolean silent) {
		this.silent = silent;
	}
	public String getActorName() {
		return actorName;
	}
	public void setActorName(String actorName) {
		this.actorName = actorName;
	}
	public String getPartName() {
		return partName;
	}
	public void setPartName(String partName) {
		this.partName = partName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((actorName == null) ? 0 : actorName.hashCode());
		result = prime * result + (isDialog ? 1231 : 1237);
		result = prime * result
				+ ((partName == null) ? 0 : partName.hashCode());
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
		ActorBean other = (ActorBean) obj;
		if (actorName == null) {
			if (other.actorName != null)
				return false;
		} else if (!actorName.equals(other.actorName))
			return false;
		if (isDialog != other.isDialog)
			return false;
		if (partName == null) {
			if (other.partName != null)
				return false;
		} else if (!partName.equals(other.partName))
			return false;
		if (silent != other.silent)
			return false;
		return true;
	}
	public boolean isDialog() {
		return isDialog;
	}
	public void setDialog(boolean isDialog) {
		this.isDialog = isDialog;
	}

}
