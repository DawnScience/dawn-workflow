/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.editors;

import org.eclipse.jface.text.Region;

public class VariableRegion extends Region {

	private VariableType variableType;
	
	public VariableRegion(int offset, int length, VariableType pType) {
		super(offset, length);
		this.variableType = pType;
	}

	public boolean isSelected() {
		return variableType==VariableType.SELECTED;
	}

	public VariableType getVariableType() {
		return variableType;
	}

	public void setVariableType(VariableType variableType) {
		this.variableType = variableType;
	}
}
