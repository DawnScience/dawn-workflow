/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.flow;

import org.dawb.passerelle.common.parameter.CellEditorParameter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

public class ExpressionParameter extends CellEditorParameter {
	
	private String  nameParameter;
	private boolean automaticExpressionCreation=true;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8999174318856900808L;
	
	private final static Logger logger = LoggerFactory.getLogger(ExpressionParameter.class);
	
	public ExpressionParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}
	
	@Override
	public CellEditor createCellEditor(Control control) {
		
		final DialogCellEditor editor = new DialogCellEditor((Composite)control) {
			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
								
				final ExpressionDialog dialog = new ExpressionDialog(cellEditorWindow.getShell(), getContainer()); // extends BeanDialog
				dialog.setAutomaticExpressionCreation(automaticExpressionCreation);
				dialog.create();
				if (nameParameter!=null) {
					dialog.setNameLabel(nameParameter);
				}
				dialog.getShell().setSize(400,435); // As needed
				dialog.getShell().setText("Create expressions");
			
				dialog.setBean(getBeanFromValue(ExpressionContainer.class));
		        final int ok = dialog.open();
		        if (ok == Dialog.OK) {
		            return getValueFromBean((ExpressionContainer)dialog.getBean());
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
		return getBeanFromValue(ExpressionContainer.class).toString();
	}

	public void setNameParameter(String string) {
		nameParameter = string;
	}

	public boolean isAutomaticExpressionCreation() {
		return automaticExpressionCreation;
	}

	public void setAutomaticExpressionCreation(boolean automaticExpressionCreation) {
		this.automaticExpressionCreation = automaticExpressionCreation;
	}	

}
