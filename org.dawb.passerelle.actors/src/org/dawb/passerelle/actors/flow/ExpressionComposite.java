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

import java.util.Map;

import org.dawnsci.common.richbeans.components.wrappers.TextWrapper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class ExpressionComposite extends Composite {

	private TextWrapper actorName,expression;
	private Label actorLabel;

	public ExpressionComposite(Composite parent, int style) {
		
		super(parent, style);
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		setLayout(new GridLayout(2, false));
		
		this.actorLabel = new Label(this, SWT.NONE);
		actorLabel.setText("Actor Name");
		
		this.actorName = new TextWrapper(this, SWT.NONE);
		actorName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		actorName.setTextLimit(64);

		final Label expressionLabel = new Label(this, SWT.NONE);
		expressionLabel.setText("Expression");
		
		this.expression = new TextWrapper(this, SWT.NONE);
		expression.setTextType(TextWrapper.TEXT_TYPE.EXPRESSION);
		expression.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
	}

	public TextWrapper getActorName() {
		return actorName;
	}

	public TextWrapper getExpression() {
		return expression;
	}

	protected void setExpressionVariables(final Map<String, Object> vars) {
		expression.setExpressionVariables(vars);
	}

	public void setNameLabel(String label) {
		actorLabel.setText(label);
		layout(new Control[]{actorLabel});
	}
}
