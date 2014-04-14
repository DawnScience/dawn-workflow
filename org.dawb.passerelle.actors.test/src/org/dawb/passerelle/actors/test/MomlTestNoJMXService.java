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

import java.io.FileInputStream;

import org.dawb.common.util.test.TestUtils;
import org.dawb.passerelle.common.project.PasserelleProjectUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.edna.pydev.extensions.utils.InterpreterUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.isencia.passerelle.workbench.model.launch.ModelRunner;

public class MomlTestNoJMXService {
	
	/**
	 * Ensure that the projects are available in this workspace.
	 * @throws Exception
	 */
	@BeforeClass
	public static void before() throws Exception {
		
//    	InterpreterUtils.createJythonInterpreter("jython", new NullProgressMonitor());
//    	InterpreterUtils.createPythonInterpreter("python", new NullProgressMonitor());
		PasserelleProjectUtils.createWorkflowProject("workflows", ResourcesPlugin.getWorkspace().getRoot(), true, null);
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}
	/**
	 * Here we test if things run without a JMX agent.
	 * @throws Throwable
	 */
	@Test
	public void testFolderLoop() throws Throwable {
		
		final String afile = TestUtils.getAbsolutePath("org.dawb.workbench.examples", "workflows/folder_example.moml");

		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		if (!workflows.exists()) {
			workflows.create(null);
			workflows.open(null);
		}
		
		final IFile moml = workflows.getFile("folder_example.moml");
		moml.create(new FileInputStream(afile), IResource.FORCE, null);
				
		
		final ModelRunner runner = new ModelRunner();
		runner.runModel(moml.getLocation().toOSString(), false);
	}	
	
}
