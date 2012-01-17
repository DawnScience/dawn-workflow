/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.views;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

class ActorValueLabelProvider extends ColumnLabelProvider {

	private int col;

	public ActorValueLabelProvider(final int col) {
		this.col = col;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (col%2==0) {
			return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		}
		return null;
	}

	
	/**
	 * 
	 */
	@Override
	public String getText(Object element) {
		final ActorValueObject var = (ActorValueObject)element;
		switch(col) {
		case 0:
			return var.getInputName();
		case 1:
			return getShortVersion(var.getInputValue());
		case 2:
			return var.getOutputName();
		case 3:
			return getShortVersion(var.getOutputValue());
		default:
			return "";
		}
	}


	private String getShortVersion(Object exampleValue) {
		if (exampleValue==null) return null;
		
		String stringValue = exampleValue.toString().trim();
		if (stringValue.indexOf('\n')<0) return stringValue;
		
		stringValue = stringValue.substring(0, stringValue.indexOf('\n'));
		return stringValue+"...";
	}
	

}
