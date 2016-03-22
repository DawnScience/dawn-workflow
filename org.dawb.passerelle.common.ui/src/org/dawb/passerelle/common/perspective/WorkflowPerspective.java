/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

/**
 *   WorkflowPerspective
 *
 *   @author gerring
 *   @date Jul 8, 2010
 *   @project org.edna.workbench.application
 **/
public class WorkflowPerspective implements IPerspectiveFactory {

	/**
	 * We do not change the id as old workspaces reference its old location.
	 */
	public static final String ID = "org.edna.workbench.application.perspective.WorkflowPerspective";
	
	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
				
		IFolderLayout navigatorFolder = layout.createFolder("navigator-folder", IPageLayout.LEFT, 0.22f, editorArea);
		navigatorFolder.addView("org.eclipse.ui.navigator.ProjectExplorer");
		navigatorFolder.addView("org.dawnsci.fileviewer.FileViewer");
       
        IFolderLayout outlineFolder = layout.createFolder("outline-folder",IPageLayout.BOTTOM,0.6f,"navigator-folder");
        outlineFolder.addView(IPageLayout.ID_OUTLINE);
        
        IFolderLayout viewFolder = layout.createFolder("view-folder",IPageLayout.BOTTOM,0.6f,editorArea);
        viewFolder.addView("com.isencia.passerelle.workbench.model.editor.ui.views.ActorAttributesView");
        viewFolder.addView("org.dawb.passerelle.views.ValueView");
        viewFolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);
        {
        	IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.RIGHT, 0.68f, IPageLayout.ID_EDITOR_AREA);
        	try {
        		folderLayout.addView("org.dawb.workbench.views.dataSetView");
        	} catch (Throwable ne) {
        		// Its allowed
        	}
        	folderLayout.addView("org.dawb.common.ui.views.dashboardView");
        	folderLayout.addView("org.dawb.passerelle.ui.documentationView");
        	folderLayout.addView("org.dawb.workbench.views.imageMonitorView");
        }
        {
//        	IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.RIGHT, 0.71f, IPageLayout.ID_EDITOR_AREA);
//        	folderLayout.addView("uk.ac.diamond.scisoft.analysis.rcp.plotView1");
//        	folderLayout.addView("uk.ac.diamond.scisoft.analysis.rcp.plotView2");
        }
//        viewFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        
        // Ensure that the run menu is visible
        layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
	}
}
