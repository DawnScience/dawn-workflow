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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.dawb.common.ui.util.GridUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ptolemy.kernel.util.NamedObj;


import uk.ac.gda.richbeans.components.selector.VerticalListEditor;
import uk.ac.gda.richbeans.dialog.BeanDialog;

public class ExpressionDialog extends BeanDialog {

	private VerticalListEditor expressions;
	
	/**
	 * Used to check expressions entered.
	 */
	private If parent;
	
	protected ExpressionDialog(Shell parentShell, NamedObj container) {
		super(parentShell);
		this.parent = (If)container;
	}

	public void setNameLabel(String nameParameter) {
		ExpressionComposite comp = (ExpressionComposite)expressions.getEditorUI();
		comp.setNameLabel(nameParameter);
		
	}

	public Control createDialogArea(Composite parent) {
		
		final Composite main = (Composite)super.createDialogArea(parent);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		expressions = new VerticalListEditor(main, SWT.NONE);
		expressions.setRequireSelectionPack(false);
		expressions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		expressions.setMinItems(0);
		expressions.setMaxItems(25);
		expressions.setDefaultName("");
		expressions.setEditorClass(ExpressionBean.class);
		expressions.setEditorUI(new ExpressionComposite(expressions, SWT.NONE));
		expressions.setNameField("outputPortName");
		expressions.setAdditionalFields(new String[]{"expression"});
		expressions.setColumnWidths(new int[]{150, 300});
		expressions.setListHeight(150);
		
		GridUtils.setVisibleAndLayout(expressions, true);
		
		return main;
	}


	public VerticalListEditor getExpressions() {
		return expressions;
	}
	
	public int open() {
		expressions.setShowAdditionalFields(true);
        int ret = super.open();
        expressions = null;
        return ret;
	}
	
	public Object getBean() {
		ExpressionContainer eBean = (ExpressionContainer)super.getBean();
		if (eBean.getExpressions().size() == 0) {
			// All output port removed from the expression container! Force expression output = true
			// TODO: Popup window
			ExpressionBean expression = new ExpressionBean();
			expression.setExpression("true");
			expression.setOutputPortName("output");
			eBean.addExpression(expression);
		}
		// Check that a output port with the name "output" exists
		int index = 0;
		boolean existOutput = false;
		ArrayList<String> listOutputPortNames = new ArrayList<String>();
		for (ExpressionBean expression : eBean.getExpressions()) {
			listOutputPortNames.add(expression.getOutputPortName());
			if (expression.getOutputPortName().equals("output")) {
				existOutput = true;
			}
			index = index + 1;
		}
		// Create a unique list (see http://www.theeggeadventure.com/wikimedia/index.php/Java_Unique_List)
		Set<String> set = new HashSet<String>(listOutputPortNames);
		ArrayList<String> uniqueListOutputPortNames = new ArrayList<String>(set);
		// We must have at least one output port name = "output"! Warn the user if not
		if (!existOutput) {
			// TODO: Popup window
			for (ExpressionBean expression : eBean.getExpressions()) {
				if (expression.getOutputPortName().equals(uniqueListOutputPortNames.get(0)))
					expression.setOutputPortName("output");
			}
			uniqueListOutputPortNames.set(0, "output");
		}
		// Retrieve a list of all output port names except the name "output"
		if  (uniqueListOutputPortNames.size() > 1) {
			String[] listNames = new String[uniqueListOutputPortNames.size() - 1];
			int index2 = 0;
			for (String name : uniqueListOutputPortNames) {
				if (!name.equals("output")) {
					listNames[index2] = name;
					index2 = index2+1;
				} 
			}
			parent.outputPortSetterBuilder.setOutputPortNames(listNames);
		} else {
			parent.outputPortSetterBuilder.setOutputPortNames(new String[] {} );
		}
		return eBean;
	}

		
}
