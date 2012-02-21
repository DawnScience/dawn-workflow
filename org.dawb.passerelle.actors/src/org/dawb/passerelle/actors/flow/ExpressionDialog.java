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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.passerelle.common.message.IVariable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.isencia.passerelle.core.Port;

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
		List<String> names = new ArrayList<String>();
		ExpressionContainer eBean = (ExpressionContainer)super.getBean();
		for (ExpressionBean expression : eBean.getExpressions()) {
			String outputPortName = expression.getOutputPortName();
			names.add(outputPortName);
		}
		String [] listNames = new String[names.size()];
		for (int i=0; i<names.size(); i++) {
			listNames[i] = names.get(i);
		}
		parent.outputPortSetterBuilder.setOutputPortNames(listNames);
		return eBean;
	}

		
		
		
	public void setBean(final Object bean) {
		
		final ExpressionContainer eBean = (ExpressionContainer)bean;

		// The list of the defined output ports
		List<Port> outputPorts = parent.outputPortSetterBuilder.getOutputPorts();
		if (outputPorts!=null && !outputPorts.isEmpty()) {
	        for (Port port : (List<Port>)outputPorts) {
	        	final String name = port.getName();
				if (!eBean.containsOutputPort(name)) {
					eBean.addExpression(new ExpressionBean(name, "true"));
				}
			}
		}
		
		super.setBean(eBean);
	}

}
