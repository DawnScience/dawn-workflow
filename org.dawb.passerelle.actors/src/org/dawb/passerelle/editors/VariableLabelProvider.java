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

import java.util.HashMap;
import java.util.Map;

import org.dawb.passerelle.actors.Activator;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class VariableLabelProvider extends ColumnLabelProvider {

	protected final int column;
	protected final Map<VARIABLE_TYPE, Image>  imageMap;
	protected final Map<VARIABLE_TYPE, String> toolTipMap;

	public VariableLabelProvider(int column) {
		this.column     = column;
		this.imageMap   = new HashMap<VARIABLE_TYPE,Image>(4);
		this.toolTipMap = new HashMap<VARIABLE_TYPE,String>(4);
		
		imageMap.put(VARIABLE_TYPE.PATH,   Activator.getImageDescriptor("icons/variable_file.gif").createImage());
		imageMap.put(VARIABLE_TYPE.SCALAR, Activator.getImageDescriptor("icons/variable_scalar.gif").createImage());
		imageMap.put(VARIABLE_TYPE.ARRAY,  Activator.getImageDescriptor("icons/variable_array.gif").createImage());
		imageMap.put(VARIABLE_TYPE.IMAGE,  Activator.getImageDescriptor("icons/variable_image.gif").createImage());
		imageMap.put(VARIABLE_TYPE.XML,    Activator.getImageDescriptor("icons/variable_xml.png").createImage());
	
		toolTipMap.put(VARIABLE_TYPE.PATH,   "A path to a file read from the previous node.");
		toolTipMap.put(VARIABLE_TYPE.SCALAR, "A scalar value such as a text string or a number.");
		toolTipMap.put(VARIABLE_TYPE.ARRAY,  "A data array with unspecified dimensions.");
		toolTipMap.put(VARIABLE_TYPE.IMAGE,  "An image, a two dimensional array.");
		toolTipMap.put(VARIABLE_TYPE.XML,    "An xml attribute, element or elements extracted using XPath.");
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (!(element instanceof IVariable)) return null;
		if (column==0) {
			return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		}
		return null;
	}

	/**
	 * 
	 */
	@Override
	public Image getImage(Object element) {
		if (!(element instanceof IVariable)) return null;
		final IVariable var = (IVariable)element;
		if (column==0) return imageMap.get(var.getVariableType());
		return null;
	}
	
	/**
	 * 
	 */
	@Override
	public String getText(Object element) {
		if (!(element instanceof IVariable)) return "";
		final IVariable var = (IVariable)element;
		switch(column) {
		case 0:
			return var.getVariableName();
		default:
			return "";
		}
	}
	
	@Override
	public String getToolTipText(Object element) {
		if (!(element instanceof IVariable)) return null;
		final IVariable var = (IVariable)element;
		return toolTipMap.get(var.getVariableType());
	}

	
	@Override
	public void dispose() {
		super.dispose();
		
		for (Image im : imageMap.values()) {
			im.dispose();
		}
	}


}
