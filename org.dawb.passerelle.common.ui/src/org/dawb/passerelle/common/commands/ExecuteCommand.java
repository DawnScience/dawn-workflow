/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.commands;

import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;

import org.dawb.common.ui.util.EclipseUtils;

import com.isencia.passerelle.model.Flow;
import com.isencia.passerelle.model.FlowManager;

/**
 *   ExecuteCommand
 *
 *   @author gerring
 *   @date Aug 17, 2010
 *   @project org.edna.passerelle.common
 **/
public class ExecuteCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        
		FlowManager flowManager = new FlowManager();
		final IFile file        = getWorkflowSelected();
		
		if (file== null) { // TODO Show warning to user
		    throw new ExecutionException("NO FILE SELECTED - INFORM USER!");
		}
		
		try {
			URL fileUrl = file.getLocation().toFile().toURL();
			Flow flow = FlowManager.readMoml(fileUrl);
	        flowManager.executeBlockingLocally(flow, new HashMap<String,String>(3));
	        return Boolean.TRUE;
	        
		} catch (Exception e) {
			throw new ExecutionException("Cannot execute "+file.getName(), e);
		}
		
	}

	private IFile getWorkflowSelected() {
		
		final IEditorPart ref = org.dawb.common.ui.util.EclipseUtils.getActivePage().getActiveEditor();
		if (ref !=null) {
			final IFile file = EclipseUtils.getIFile(ref.getEditorInput());
			if (file.getName().endsWith(".moml")) return file;
		}
		
		ISelectionService service = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		if (service!=null) {
			IStructuredSelection  structured = (IStructuredSelection)service.getSelection("org.eclipse.ui.navigator.ProjectExplorer");
			if (structured==null) structured = (IStructuredSelection)service.getSelection("org.eclipse.jdt.ui.PackageExplorer");
			IFile file = (IFile) structured.getFirstElement();
			if (file.getName().endsWith(".moml")) return file;
		}
		
		return null;
	}

}
