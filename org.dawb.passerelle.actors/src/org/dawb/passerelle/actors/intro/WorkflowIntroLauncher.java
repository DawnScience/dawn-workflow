/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.intro;

import org.dawb.common.ui.util.EclipseUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkflowIntroLauncher implements IWorkbenchWindowActionDelegate{

    private static Logger logger = LoggerFactory.getLogger(WorkflowIntroLauncher.class);
    
	@Override
	public void run(IAction action) {
		try {
			PlatformUI.getWorkbench().showPerspective("org.edna.workbench.application.perspective.WorkflowPerspective",PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		    
            final boolean isAWorkflowProject = EclipseUtils.isProjectExisting("org.dawb.passerelle.common.PasserelleNature");			
			if (!isAWorkflowProject) {
				EclipseUtils.openWizard("org.edna.passerelle.common.project.PasserelleWizard", true); // Quite old wizard id!
			}

		} catch (Exception e) {
			logger.error("Cannot show new workflow project wizard!", e);
		} 	

	}


	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}


}
