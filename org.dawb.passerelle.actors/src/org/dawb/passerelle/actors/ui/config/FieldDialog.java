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

import org.dawb.common.ui.util.GridUtils;
import org.dawnsci.common.richbeans.components.selector.BeanSelectionEvent;
import org.dawnsci.common.richbeans.components.selector.BeanSelectionListener;
import org.dawnsci.common.richbeans.components.selector.VerticalListEditor;
import org.dawnsci.common.richbeans.components.wrappers.TextWrapper;
import org.dawnsci.common.richbeans.dialog.BeanDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ptolemy.kernel.util.NamedObj;

import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class FieldDialog extends BeanDialog {

	private VerticalListEditor fields;
	private TextWrapper        customLabel;
	
	protected FieldDialog(Shell parentShell, NamedObj container) {
		super(parentShell);
	}
	
	public Control createDialogArea(Composite parent) {
		
		final Composite main = (Composite)super.createDialogArea(parent);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	       		
		final TabFolder tabFolder = new TabFolder(main, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final TabItem fieldsTab = new TabItem(tabFolder, SWT.NONE);
		fieldsTab.setText("Fields");
		
        final Composite fieldsComp = new Composite(tabFolder, SWT.NONE);
        fieldsComp.setLayout(new GridLayout(1, false));
        fieldsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        fieldsTab.setControl(fieldsComp);
		
		final Label label = new Label(fieldsComp, SWT.WRAP);
		label.setText("Create fields that the user should edit here. Their default values can be set but if an upsteam actor has set the value that will populate the field.");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		fields = new VerticalListEditor(fieldsComp, SWT.NONE);
		fields.setRequireSelectionPack(false);
		fields.setListenerName("Fields Listener");
		fields.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fields.setMinItems(0);
		fields.setMaxItems(25);
		fields.setDefaultName("x");
		fields.setEditorClass(FieldBean.class);
		
		final FieldComposite fieldComp = new FieldComposite(fieldsComp, SWT.NONE);
		fieldComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		fields.setEditorUI(fieldComp);
		fields.setNameField("variableName");
		fields.setAdditionalFields(new String[]{"defaultValue"});
		fields.setColumnWidths(new int[]{100, 300});
		fields.setListHeight(120);
		fields.getViewer().getControl().setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		fields.addBeanSelectionListener(new BeanSelectionListener() {
			@Override
			public void selectionChanged(BeanSelectionEvent evt) {
				fieldComp.updateVisibleProperties();
			}
		});
		
		GridUtils.setVisible(fields, true);
		fields.getParent().layout(new Control[]{fields});
		
		final TabItem labelTab = new TabItem(tabFolder, SWT.NONE);
		labelTab.setText("Custom Label");

		customLabel = new TextWrapper(tabFolder, SWT.MULTI|SWT.WRAP|SWT.LEFT);
		labelTab.setControl(customLabel);
		customLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		return main;
	}


	public VerticalListEditor getFields() {
		return fields;
	}
	
	public TextWrapper getCustomLabel() {
		return customLabel;
	}
}
