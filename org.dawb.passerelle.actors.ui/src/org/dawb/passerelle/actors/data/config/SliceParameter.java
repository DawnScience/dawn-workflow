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

import java.io.File;

import org.dawb.passerelle.common.parameter.CellEditorParameter;
import org.dawnsci.slicing.api.SliceDialog;
import org.dawnsci.slicing.api.system.AxisType;
import org.dawnsci.slicing.api.system.DimsData;
import org.dawnsci.slicing.api.system.DimsDataList;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.io.IDataHolder;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;

import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;


public class SliceParameter extends CellEditorParameter implements CellEditorAttribute {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4735320242872076263L;
	
	
	private final static Logger logger = LoggerFactory.getLogger(SliceParameter.class);
	
	public SliceParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}
	
	@Override
	public CellEditor createCellEditor(Control control) {
		
		// This forces the parent to be this actor which for now is correct.
		// Fix later if required.
		final ISliceInformationProvider source = (ISliceInformationProvider)getContainer();
		final String[]         names  = source.getDataSetNames();
		if (names==null|| names.length!=1) {
			MessageDialog.openError(control.getShell(),
					                "No data set chosen",
					                "Please choose one data set to slice for the pipeline.\n\n"+
					                "To do this open the 'Data Sets' parameter and choose the\n"+
					                "image stack (for instance) you would like to use then\n"+
					                "open the 'Data Set Slice' and choose how to slice this\n"+
					                "stack, each slice will enter the pipeline separately.");
			return null;
		}
		
		final String path = source.getSourcePath();
		if (path==null || !((new File(path)).exists())) {
			MessageDialog.openError(control.getShell(),
					                "No file chosen",
					                "Please choose an existing file to slice for the pipeline.\n\n"+
					                "To do this open the 'Path' parameter and choose an existing\n"+
					                "file. Then the slicing can read the file to get the dimensionality.");
			return null;
		}
		
		final DialogCellEditor editor = new DialogCellEditor((Composite)control) {
			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
								
				final SliceDialog dialog = new SliceDialog(cellEditorWindow.getShell(), false); // extends BeanDialog
				dialog.create();
				dialog.getShell().setSize(400,450); // As needed
				dialog.getShell().setText("Create Slices for Import");
			
				try {
					dialog.setData(names[0], path, null);
				} catch (Throwable e) {
					String message = e.getMessage();
					if (message==null || "".equals(message)) message = "Cannot slice data in "+path;
					ErrorDialog.openError(cellEditorWindow.getShell(), "Extraction error", e.getMessage(), new Status(IStatus.ERROR, "org.dawb.passerelle.actors", "", e));
					return null;
				}
				
				DimsDataList ddl = (DimsDataList)getBeanFromValue(DimsDataList.class);
				if (ddl == null) ddl = new DimsDataList();
				
				if (ddl.isEmpty()) {
					try {
					    final IDataHolder  dh = LoaderFactory.getData(path);
					    final ILazyDataset lz = dh.getLazyDataset(names[0]);
					    for (int i = 0; i < lz.getRank(); i++) {
					    	ddl.add(new DimsData(i));
					    }
					    
					    ddl.getDimsData(0).setSliceRange("all");
					    if (ddl.size()==2) {
					    	ddl.getDimsData(1).setPlotAxis(AxisType.X);
					    	
					    } else if (ddl.size()>2) {
					    	ddl.getDimsData(1).setPlotAxis(AxisType.Y);
					    	ddl.getDimsData(2).setPlotAxis(AxisType.X);
					    	for (int i = 3; i < ddl.size(); i++) {
					    		ddl.getDimsData(i).setSlice(0);
							}
					    }
					    
					} catch (Exception neOther) {
						logger.error("Cannot set up slice parameter!", neOther);
					}
					
				}
				dialog.setDimsDataList(ddl);
				dialog.setRangesAllowed(true);
				
		        final int ok = dialog.open();
		        if (ok == Dialog.OK) {
		            return getValueFromBean(dialog.getDimsDataList());
		        }
		        
		        return null;
			}
		    protected void updateContents(Object value) {
		        if ( getDefaultLabel() == null) {
					return;
				}
		        getDefaultLabel().setText(getRendererText());
		    }

		};
		
		
		return editor;
	}

	/**
	 * May need to cache here but JFace already does a better job of this than swing.
	 */
	@Override
	public String getRendererText() {
		
		int[] shape = null;
		try {
			final ISliceInformationProvider source = (ISliceInformationProvider)getContainer();
			final IMetaData meta  = LoaderFactory.getMetaData(source.getSourcePath(), null);
	        shape  = meta.getDataShapes().get(source.getDataSetNames()[0]);
		} catch (Exception ne) {
			shape = null;
		}
		
		if (getExpression()==null||"".equals(getExpression())) return "";
		DimsDataList dList = (DimsDataList)getBeanFromValue(DimsDataList.class);
		return dList.toString(shape);
		
	}	

}
