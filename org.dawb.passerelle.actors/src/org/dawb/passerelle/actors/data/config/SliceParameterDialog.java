/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.data.config;

import org.dawb.common.ui.slicing.DimsDataList;
import org.dawb.common.ui.slicing.SliceComponent;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.io.DataHolder;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.analysis.monitor.IMonitor;

public class SliceParameterDialog extends Dialog {
	
	private static Logger logger = LoggerFactory.getLogger(SliceParameterDialog.class);
	
	private DimsDataList   dimsDataList;
	private SliceComponent sliceComponent;
	
	protected SliceParameterDialog(Shell parentShell, NamedObj container) {
		super(parentShell);
		setShellStyle(SWT.RESIZE|SWT.DIALOG_TRIM);
	}
	
	public Control createDialogArea(Composite parent) {
		
		this.sliceComponent = new SliceComponent("org.dawb.workbench.views.h5GalleryView");
		final Control slicer = sliceComponent.createPartControl(parent);
		slicer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sliceComponent.setAxesVisible(false);

		sliceComponent.setVisible(true);
		
		return parent;
	}

	/**
	 * Sets the data which the parameter should slice.
	 * @param dataSetName
	 * @param filePath
	 * @throws Exception 
	 */
	public void setData(final String   dataSetName,
			            final String   filePath,
			            final IMonitor monitor) throws Exception {
		
		final DataHolder holder = LoaderFactory.getData(filePath, monitor);
		final ILazyDataset lazy = holder.getLazyDataset(dataSetName);
		sliceComponent.setData(lazy, dataSetName, filePath);        
	}

	public DimsDataList getDimsDataList() {
		return dimsDataList;
	}

	/**
	 * Examples "[0, X, Y]",   "[0, 0, X]",  "[1;10;1, 5, X]"
	 * @param persistedString
	 */
	public void setDimsDataList(final DimsDataList dimsDataList) {
		
		if (dimsDataList==null)    return;
		if (dimsDataList.size()<1) return;
		try {
			sliceComponent.setDimsDataList(dimsDataList);
		} catch (Exception e) {
			logger.error("Cannot set persisted string in slice component!", e);
		}
	}

	protected void okPressed() {
		try {
			this.dimsDataList = sliceComponent.getDimsDataList();
		} catch (Exception e) {
			logger.error("Cannot get persisted string in slice component!", e);
		}
		super.okPressed();
	}

}
