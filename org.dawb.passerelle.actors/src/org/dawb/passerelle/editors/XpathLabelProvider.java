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

import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.XPathVariable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

public class XpathLabelProvider extends VariableLabelProvider {

	protected final Map<VARIABLE_TYPE, String> messageMap;
	private Font uneditableFont;
	private XPathEditor editor;

	public XpathLabelProvider(XPathEditor ed, int column, final Font current) {
		super(column);
		
		this.editor     = ed;
		this.messageMap = new HashMap<VARIABLE_TYPE,String>(4);
	
		messageMap.put(VARIABLE_TYPE.PATH,   "A path, '");
		messageMap.put(VARIABLE_TYPE.SCALAR, "A scalar value, '");
		messageMap.put(VARIABLE_TYPE.ARRAY,  "An array, '");
		messageMap.put(VARIABLE_TYPE.IMAGE,  "An image, '");
		messageMap.put(VARIABLE_TYPE.XML,    "An uneditable xml attribute tag, '");
		
		toolTipMap.put(VARIABLE_TYPE.XML,    "An xml attribute, element or elements extracted using XPath. Note you can put other variables in the name, for instance 'x_${file_name}'");

		final FontData curDat = current.getFontData()[0];
		this.uneditableFont = new Font(current.getDevice(), curDat.getName(), curDat.getHeight(), SWT.ITALIC);
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (!(element instanceof IVariable)) return null;
		final IVariable var = (IVariable)element;
		if (column==0) {
			return var.getErrorMessage()==null
			       ? Display.getCurrent().getSystemColor(SWT.COLOR_BLUE)
			       : Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		}
		if (column==1) {
			if ( var instanceof XPathVariable ) {
				if (editor.getActor()!=null) {
					try {
						if (!editor.getActor().isUpstreamVariable(var.getVariableName())) {
							return Display.getCurrent().getSystemColor(SWT.COLOR_BLACK); 
						} else {
							return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
						}
					} catch (Exception e) {
						return Display.getCurrent().getSystemColor(SWT.COLOR_BLACK); 
					}
				} else {
					return Display.getCurrent().getSystemColor(SWT.COLOR_BLACK); 
				}
			} else {
				return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	public Font getFont(Object element) {
		if (!(element instanceof IVariable)) return null;
		final IVariable var = (IVariable)element;
		if (var instanceof XPathVariable) {
			return editor.getActor().isUpstreamVariable(var.getVariableName())
			       ? uneditableFont : null;
		}
		return uneditableFont;
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
		case 1:
			if (var instanceof XPathVariable) {
				XPathVariable xVar = (XPathVariable)var;
				return xVar.getxPath();
			}
			return messageMap.get(var.getVariableType())+var.getExampleValue()+"'";
		case 2:
			if (var instanceof XPathVariable) {
				XPathVariable xVar = (XPathVariable)var;
				return xVar.getRename();
			}
			
		default:
			return "";
		}
	}
	
	
	@Override
	public String getToolTipText(Object element) {
		
		final IVariable var = (IVariable)element;
		String tip = toolTipMap.get(var.getVariableType());
		if (column==2) {
			tip = "The top level tag extracted can be renamed.\nUsed when the object extracted is renamed before being substituted into the next node.";
		}
		if (var.getErrorMessage()!=null) {
			return tip+"\n"+var.getErrorMessage();
		}
		return tip;
	}

}
