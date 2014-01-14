/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.ui.editors;

import org.eclipse.jface.text.Position;

public class VariablePosition extends Position {

	private VariableType variableType;
	public VariablePosition(int i, int j) {
		super(0,0);
	}
	public VariableType getVariableType() {
		return variableType;
	}
	public void setVariableType(VariableType variableType) {
		this.variableType = variableType;
	}
	public boolean isSelected() {
		return variableType==VariableType.SELECTED;
	}

}
