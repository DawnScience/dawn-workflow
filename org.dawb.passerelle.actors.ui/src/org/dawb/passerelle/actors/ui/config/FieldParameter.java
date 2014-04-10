/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ui.config;

import org.dawb.passerelle.common.parameter.CellEditorParameter;
import org.dawnsci.common.richbeans.beans.BeanUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

public class FieldParameter extends CellEditorParameter {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8999174318856900808L;
	
	private final static Logger logger = LoggerFactory.getLogger(FieldParameter.class);
	
	public FieldParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}
	
	@Override
	public CellEditor createCellEditor(Control control) {
		
		final DialogCellEditor editor = new DialogCellEditor((Composite)control) {
			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
								
				final FieldDialog dialog = new FieldDialog(cellEditorWindow.getShell(), getContainer()); // extends BeanDialog
				dialog.create();
				dialog.getShell().setSize(550,750); // As needed
				dialog.getShell().setText("Create user fields");
			
				dialog.setBean(getBeanFromValue(FieldContainer.class));
		        final int ok = dialog.open();
		        if (ok == Dialog.OK) {
		            return getValueFromBean((FieldContainer)dialog.getBean());
		        }
		        
		        return null;
			}
		    protected void updateContents(Object value) {
		        if ( getDefaultLabel() == null) {
					return;
				}
		        getDefaultLabel().setText(getRendererText());
		    }

		};
		
		
		return editor;
	}

	/**
	 * May need to cache here but JFace already does a better job of this than swing.
	 */
	@Override
	public String getRendererText() {
		return getBeanFromValue(FieldContainer.class).toString();
	}	

}
