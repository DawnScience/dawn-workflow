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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
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
	private boolean automaticExpressionCreation=true;
	
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
		expressions.setDefaultName(getOutputPortName());
		expressions.setEditorClass(ExpressionBean.class);
		expressions.setEditorUI(createExpressionComposite());
		expressions.setNameField("outputPortName");
		expressions.setAdditionalFields(new String[]{"expression"});
		expressions.setColumnWidths(new int[]{150, 300});
		expressions.setListHeight(150);
		
		GridUtils.setVisibleAndLayout(expressions, true);
		
		return main;
	}

	private Object createExpressionComposite() {
		
		final ExpressionComposite expressionComposite = new ExpressionComposite(expressions, SWT.NONE);
		
		if (automaticExpressionCreation) {
			final Map<String,Object> values = new HashMap<String,Object>(7);
			final List<IVariable>    vars   = parent.getInputVariables();
			for (IVariable var : vars) {
				Object value = var.getExampleValue();
				if (value instanceof String) {
					try {
						value = Double.parseDouble((String)value);
					} catch (Exception igonred) {
						// Nothing
					}
				}
				values.put(var.getVariableName(), value);
			}
			expressionComposite.setExpressionVariables(values);
		}
		return expressionComposite;
	}


	private String getOutputPortName() {
		final List<Port> outputPorts = parent.outputPortCfgExt.getOutputPorts();
		if (outputPorts!=null && !outputPorts.isEmpty()) return (outputPorts.get(0).getName());
		return "outputPort";
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
	
	public void setBean(final Object bean) {
		
		final ExpressionContainer eBean = (ExpressionContainer)bean;

		// First item the default output port
		eBean.addExpression(new ExpressionBean("output", "true"));

		// Then the defined output ports
		List<Port> outputPorts = parent.outputPortCfgExt.getOutputPorts();
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

	public boolean isAutomaticExpressionCreation() {
		return automaticExpressionCreation;
	}

	public void setAutomaticExpressionCreation(boolean automaticExpressionCreation) {
		this.automaticExpressionCreation = automaticExpressionCreation;
	}
}
