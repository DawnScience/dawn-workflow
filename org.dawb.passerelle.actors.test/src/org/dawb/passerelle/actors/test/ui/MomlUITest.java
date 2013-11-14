/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.test.ui;

import java.io.File;
import java.io.FileInputStream;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.util.test.TestUtils;
import org.dawb.passerelle.actors.test.Activator;
import org.dawb.passerelle.common.utils.ModelUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.edna.pydev.extensions.utils.InterpreterUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.isencia.passerelle.workbench.model.editor.ui.editor.PasserelleModelMultiPageEditor;

public class MomlUITest {
	
	/**
	 * Ensure that the projects are available in this workspace.
	 * @throws Exception
	 */
	@BeforeClass
	public static void before() throws Exception {
		
		InterpreterUtils.createPythonInterpreter("python", "python", new NullProgressMonitor());
		ModelUtils.createWorkflowProject("workflows", ResourcesPlugin.getWorkspace().getRoot(), true, null);
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	/**
	 * runs all the example moml files. this test must be run
	 * with a dawb workspace as the workspace
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExampleMomlModels() throws Exception {
		
		
		// We choose the workflows perspective
		final IWorkbench bench = PlatformUI.getWorkbench();
		bench.showPerspective("org.edna.workbench.application.perspective.WorkflowPerspective", bench.getActiveWorkbenchWindow());
		
		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		final IFolder  examples  = (IFolder)workflows.getFolder("examples");

		final IResource[] momls = examples.members();
		for (int i = 0; i < momls.length; i++) {
			final IEditorPart part = EclipseUtils.openEditor((IFile)momls[i]);
			if (part==null) throw new Exception("Did not open part for "+momls[i]);
			
			EclipseUtils.getPage().setPartState(EclipseUtils.getPage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
			
			EclipseUtils.delay(1000);
			
			if (part instanceof PasserelleModelMultiPageEditor) {
				PasserelleModelMultiPageEditor me = (PasserelleModelMultiPageEditor)part;
				me.setActivePage(1);
			}
			
			EclipseUtils.delay(1000);
			EclipseUtils.getPage().closeAllEditors(false);
		}	

	}
	/**
	 * runs all the example moml files. this test must be run
	 * with a dawb workspace as the workspace
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHeadlessMomlFiles() throws Exception {
		
		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");

		final String afile = TestUtils.getAbsolutePath(Activator.getDefault().getBundle(), "src/org/dawb/passerelle/actors/test/");
		final File   dir   = new File(afile);
		
		final File[] momls = dir.listFiles();
		
		for (int i = 0; i < momls.length; i++) {
			
			final File moml = momls[i];
			if (!moml.getName().endsWith(".moml")) continue;
			
			final IFile file = workflows.getFile(moml.getName());
			file.create(new FileInputStream(moml), true, null);
			
			final PasserelleModelMultiPageEditor part = (PasserelleModelMultiPageEditor)EclipseUtils.openEditor(file);
			if (part==null) throw new Exception("Did not open part for "+momls[i]);
			
			EclipseUtils.getPage().setPartState(EclipseUtils.getPage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
			
			EclipseUtils.delay(1000);
			
			if (!moml.getName().contains("_bad")&&part.isParseError()) {
				throw new Exception("Cannot parse "+file);
			}
			
			if (part instanceof PasserelleModelMultiPageEditor) {
				PasserelleModelMultiPageEditor me = (PasserelleModelMultiPageEditor)part;
				me.setActivePage(1);
			}

			EclipseUtils.delay(1000);
			EclipseUtils.getPage().closeAllEditors(false);
		}	

	}
	
}
