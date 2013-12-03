/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.preferences;

import org.dawb.passerelle.common.Activator;
import org.dawnsci.common.richbeans.components.scalebox.RangeBox;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DataAnalysisPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore preferencesStoreWB;//, preferencesStoreWF;
	private RangeBox workbenchPort;//, workflowPort;
	private String wbProp, wfProp;

	public DataAnalysisPreferencePage() {
		this(null);
	}

	public DataAnalysisPreferencePage(String title) {
		this(title, null);
	}

	/**
	 * @wbp.parser.constructor
	 */
	public DataAnalysisPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
		wbProp = PreferenceConstants.REMOTE_WORKBENCH_PORT;
        //wfProp = com.isencia.passerelle.workbench.model.activator.PreferenceConstants.REMOTE_MANAGER_PORT;
	}

	@Override
	public void init(IWorkbench workbench) {
		preferencesStoreWB = Activator.getDefault().getPreferenceStore();
		//preferencesStoreWF = com.isencia.passerelle.workbench.model.activator.Activator.getDefault().getPreferenceStore();
	}

	@Override
	protected Control createContents(final Composite parent) {
		
		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		
		this.workbenchPort = new RangeBox(comp, SWT.NONE);
		workbenchPort.setIntegerBox(true);
		workbenchPort.setLabel("Port for workbench service   ");
		workbenchPort.setToolTipText("The workbench service exposes the workbench as a local service for the workflow process to connect to. This allows the workflow to interact with the user interface.");
		workbenchPort.setIntegerValue(preferencesStoreWB.getInt(wbProp));
		workbenchPort.setMinimum(0);
		workbenchPort.setMaximum(65000);
		workbenchPort.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		workbenchPort.setButtonVisible(false);
		
//		this.workflowPort = new RangeBox(comp, SWT.NONE);
//		workflowPort.setIntegerBox(true);
//		workflowPort.setLabel("Port for workflow service     ");
//		workflowPort.setToolTipText("The workflow service exposes the workflow as a local service for the workbench process to connect to. This allows the workbench to interact with the workflow process.");
//		workflowPort.setIntegerValue(preferencesStoreWF.getInt(wfProp));
//		workflowPort.setMinimum(0);
//		workflowPort.setMaximum(65000);
//		workflowPort.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
//		workflowPort.setButtonVisible(false);

		// HACK
		parent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				parent.getShell().setSize(parent.getShell().getSize().x, parent.getShell().getSize().y+1);

			}
		});

		return comp;
	}
	
	@Override
	public boolean performOk() {

		preferencesStoreWB.setValue(wbProp, workbenchPort.getIntegerValue());
		//preferencesStoreWF.setValue(wfProp, workflowPort.getIntegerValue());

		return super.performOk();
	}
	
	@Override
	protected void performDefaults() {
		workbenchPort.setIntegerValue(preferencesStoreWB.getInt(wbProp));
		//workflowPort.setIntegerValue(preferencesStoreWF.getInt(wfProp));
		super.performDefaults();
	}

}
