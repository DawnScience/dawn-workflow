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

import java.util.Map;
import java.util.Queue;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import uk.ac.gda.common.rcp.util.DialogUtils;

/**
 * A dialog allowing an implementation of IRemoteWorkbenchPart to run
 * inside it.
 * 
 * @author fcp94556
 *
 */
public class UserInputDialog extends Dialog implements IRemoteWorkbenchPart, UserModifyComposite.Closeable {

	private final IRemoteWorkbenchPart deligate;
	private Link label;

	public UserInputDialog(final Shell parentShell, final IRemoteWorkbenchPart remotePartImpl) {
		
		super(parentShell);
		
		setShellStyle(SWT.RESIZE | SWT.TITLE);
		this.deligate = remotePartImpl;
	}
	
	protected Control createDialogArea(Composite container) {
		
		final Composite top = new Composite(container, SWT.NONE);
		top.setLayout(new GridLayout(1, false));
		top.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		this.label  = new Link(top, SWT.LEFT);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		label.setText("Please check and/or set values, then <a>continue</a> or <a>stop</a> the workflow.");
		label.addSelectionListener(new SelectionListener() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.text.contains("stop")) {
					deligate.stop();
				} else {
					deligate.confirm();
				}
				close();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		deligate.createRemotePart(container, this);
		return ((ColumnViewer)deligate.getViewer()).getControl();
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,     "Continue",  false);
		createButton(parent, IDialogConstants.CANCEL_ID, "Stop",      false);
	}
	
	public boolean close() {
		
        if (getReturnCode()==OK) {
        	deligate.confirm();
        }
        return super.close();
	}

	@Override
	public void setPartName(String partName) {
		if (getShell()!=null&&!getShell().isDisposed()) getShell().setText(partName);
	}

	@Override
	public void setQueue(Queue<Object> valueQueue) {
		deligate.setQueue(valueQueue);
	}

	@Override
	public void setValues(Map<String, String> values) {
		deligate.setValues(values);
		if (deligate.isMessageOnly()) {
			GridUtils.setVisible(label, false);
			label.getParent().layout(new Control[]{label});
			
			if (getShell()!=null && getParentShell()!=null) {
				getShell().setSize(500,300);
				DialogUtils.centerDialog(getParentShell(), getShell());
			}
		}
	}

	@Override
	public void setConfiguration(String configuration) throws Exception {
		deligate.setConfiguration(configuration);
	}

	@Override
	public void setUserObject(Object userObject) {
		deligate.setUserObject(userObject);
	}

	@Override
	public void stop() {
		deligate.stop();
	}

	@Override
	public void confirm() {
		deligate.confirm();
	}

	@Override
	public Object getViewer() {
		return deligate.getViewer();
	}

	@Override
	public boolean isMessageOnly() {
		return deligate.isMessageOnly();
	}
	@Override
	public void setRemoteFocus() {
		deligate.setRemoteFocus();
	}
	@Override
	public void dispose() {
		deligate.dispose();
	}

	@Override
	public void initializeMenu(Object iActionBars) {
		deligate.initializeMenu(iActionBars);
	}

	@Override
	public void createRemotePart(Object container, Closeable closeable) {
		// TODO Auto-generated method stub
		
	}

}
