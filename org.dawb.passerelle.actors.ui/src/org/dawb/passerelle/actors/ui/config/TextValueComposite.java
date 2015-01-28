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

import org.dawnsci.common.richbeans.beans.IFieldWidget;
import org.dawnsci.common.richbeans.components.wrappers.TextWrapper;
import org.dawnsci.common.richbeans.event.ValueListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class TextValueComposite extends Composite {

	private TextWrapper textValue;

	public TextValueComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label lblValue = new Label(this, SWT.NONE);
		lblValue.setText("Value");
		
		textValue = new TextWrapper(this, SWT.BORDER) {
			public void addValueListener(ValueListener l) {
				// Because we are nested we ignore the super container.
				if (l.getValueListenerName().equals("Fields Listener")) return;
				super.addValueListener(l);
			}
		};
		textValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	public IFieldWidget getTextValue() {
		return textValue;
	}

}
