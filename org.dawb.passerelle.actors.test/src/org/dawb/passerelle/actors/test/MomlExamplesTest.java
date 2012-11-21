/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.test;

import java.io.File;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.common.WorkbenchServiceManager;
import org.dawb.passerelle.common.utils.ModelUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.edna.pydev.extensions.utils.InterpreterUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.isencia.passerelle.workbench.model.launch.ModelRunner;

/**
 * Attempts to run all the examples in headless mode as they would be
 * in the user interface but in headless mode.
 * 
 * @author gerring
 *
 */
public class MomlExamplesTest {
	
	/**
	 * Ensure that the projects are available in this workspace.
	 * @throws Exception
	 */
	@BeforeClass
	public static void before() throws Exception {
		
//    	InterpreterUtils.createJythonInterpreter("jython", new NullProgressMonitor());
//    	InterpreterUtils.createPythonInterpreter("python", new NullProgressMonitor());
		ModelUtils.createWorkflowProject("workflows", ResourcesPlugin.getWorkspace().getRoot(), true, null);
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		WorkbenchServiceManager.startTestingWorkbenchService();
		
		// Because we run tango tests we set the system into mock mode
		final ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.dawb.common.ui");
		store.setValue("org.dawb.remote.session.mock", true);

	}

	/**
	 * runs all the example moml files. this test must be run
	 * with a dawb workspace as the workspace
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExampleMomlModels() throws Exception {
		
		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		final String   wkDir     = workflows.getLocation().toOSString();
		final File  dir = new File(wkDir+"/examples");
		final File[] momls = dir.listFiles();
		
		final long startSize = Runtime.getRuntime().totalMemory();

		for (int i = 0; i < momls.length; i++) {

			final File moml = momls[i];
			try {
				final ModelRunner runner = new ModelRunner();
				runner.runModel(moml.getAbsolutePath(), false);
			} catch (Exception ne) {
				throw new Exception("Cannot run "+moml, ne);
			}
		}

		System.gc();
		
 		EclipseUtils.delay(1000);

 		final long endSize = Runtime.getRuntime().totalMemory();
 		final long leak    = (endSize-startSize);
 		if (leak>700000000) throw new Exception("The memory leak is too large! It is "+leak);
		System.out.println("The memory leak opening example workflows is "+leak);
	}	
	
	@AfterClass
	public static void after() {
		WorkbenchServiceManager.stopWorkbenchService();
	}
}
