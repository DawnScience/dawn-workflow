/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.ui.editors;

import java.util.Map;
import java.util.Queue;

import org.dawb.common.ui.util.DialogUtils;
import org.dawb.workbench.jmx.IDeligateWorkbenchPart;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog allowing an implementation of IRemoteWorkbenchPart to run
 * inside it.
 * 
 * @author fcp94556
 *
 */
public class UserInputDialog extends Dialog implements IRemoteWorkbenchPart, IRemoteWorkbenchPart.Closeable, IAdaptable {

	private final IDeligateWorkbenchPart deligate;

	public UserInputDialog(final Shell parentShell, final IDeligateWorkbenchPart remotePartImpl) {
		
		super(parentShell);
		
		setShellStyle(SWT.RESIZE | SWT.SHELL_TRIM );
		this.deligate = remotePartImpl;
	}
	
	protected Control createDialogArea(Composite container) {
				
		deligate.createRemotePart(container, this);
		Object viewer = deligate.getViewer();
		if (viewer instanceof ColumnViewer) {
			return ((ColumnViewer)viewer).getControl();
		} else if (viewer instanceof IPlottingSystem) {
			return ((IPlottingSystem)viewer).getPlotComposite();	
		} else {
			return container;
		}
	}
	
	public Object getAdapter(Class adapter) {
		if (deligate instanceof IAdaptable) return ((IAdaptable)deligate).getAdapter(adapter);
		return null;
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
	
	public int open() {
		int ret = super.open();
		deligate.setRemoteFocus();
		return ret;
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

}
