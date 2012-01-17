/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.test.edna;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.util.test.TestUtils;
import org.dawb.hdf5.HierarchicalDataFactory;
import org.dawb.hdf5.IHierarchicalDataFile;
import org.dawb.passerelle.actors.test.Activator;
import org.dawb.passerelle.common.WorkbenchServiceManager;
import org.dawb.passerelle.common.utils.ModelUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.edna.pydev.extensions.utils.InterpreterUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.isencia.passerelle.workbench.model.launch.ModelRunner;

/**
 * Attempts to run all the examples in headless mode as they would be
 * in the user interface but in headless mode.
 * 
 * @author svensson
 *
 */
public class MomlEdnaTest {
	
	/**
	 * Ensure that the projects are available in this workspace.
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		
    	InterpreterUtils.createJythonInterpreter("jython", new NullProgressMonitor());
    	InterpreterUtils.createPythonInterpreter("python", new NullProgressMonitor());
		ModelUtils.createWorkflowProject("workflows", ResourcesPlugin.getWorkspace().getRoot(), true, null);
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		WorkbenchServiceManager.startWorkbenchService();
	}

	/**
	 * runs all the EDNA moml test files. this test must be run
	 * with a dawb workspace as the workspace
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEdnaPeakSearchv1_0() throws Exception  {		
		String momlFile = "edna_peak_search_test.moml";
		String relTestDir = "src/org/dawb/passerelle/actors/test/edna";
		boolean requireError = false;
		String resultFile = "edna_peak_search_test.h5";
		int noResults = 7;
		this.runEdnaMomlModel(momlFile, relTestDir, null, requireError, resultFile, noResults);
	}	
		
	@Test
	public void testEdnaInterfacev2_2() throws Exception  {		
		String momlFile = "edna_interfacev2_2_test.moml";
		String relTestDir = "src/org/dawb/passerelle/actors/test/edna";
		List<String> listXmlFiles = Arrays.asList("XSDataInputInterfacev2_2.xml", "XSDataOutputInterfacev2_2.properties" ); 
		boolean requireError = false;
		String resultFile = "edna_interfacev2_2_test.h5";
		int noResults = 15;
		this.runEdnaMomlModel(momlFile, relTestDir, listXmlFiles, requireError, resultFile, noResults);
	}	

	public void runEdnaMomlModel(String momlFile, String relPath, List<String> listXmlFiles, boolean requireError, String resultFile, int noResults) throws Exception {
		final long startSize = Runtime.getRuntime().totalMemory();

		final String absPathTestDir = TestUtils.getAbsolutePath(Activator.getDefault().getBundle(), relPath);
		final String afile = absPathTestDir+"/"+momlFile;

		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		if (!workflows.exists()) {
			workflows.create(new NullProgressMonitor());
			workflows.open(new NullProgressMonitor());
		}
		
		final IFile moml = workflows.getFile((new File(afile)).getName());
		if (moml.exists()) moml.delete(true, new NullProgressMonitor());
		moml.create(new FileInputStream(afile), IResource.FORCE, new NullProgressMonitor());
		
		// Copy XML/Property files
		if (listXmlFiles != null) {
			IFolder ednaXmlDir = workflows.getFolder("edna-xml");
			if (!ednaXmlDir.exists()) {
				ednaXmlDir.create(true, true, new NullProgressMonitor());
			}
			for (String xmlFile : listXmlFiles) {
				String xmlFileAbsPath = absPathTestDir+"/"+xmlFile;
				// create a new file
				IFile newFile = ednaXmlDir.getFile(xmlFile);
				FileInputStream fileStream = new FileInputStream(xmlFileAbsPath);
				newFile.create(fileStream, false, null);
				// create closes the file stream, so no worries.
			}
		}
		
		final ModelRunner runner = new ModelRunner();

		
		if (requireError) {
			// This should raise an exception - we are testing that it does!
			boolean illegalReportedException = false;
			try {
			    runner.runModel(moml.getLocation().toOSString(), false);
			} catch (Exception ne) {
				illegalReportedException = true;
			}
			if (!illegalReportedException) throw new Exception(momlFile+" should not pass!");
		
		} else {
			runner.runModel(moml.getLocation().toOSString(), false);	
		}

		IFolder output = workflows.getFolder("output");
		if (!output.exists()) throw new Exception("Cannot find output folder!");
       
		//TODO: Use proper Java path concatenation!
		final File h5 = new File(output.getLocation().toOSString() + "/" + resultFile);
		System.out.println(h5.toString()); 
        if (h5==null||!h5.exists()) throw new Exception("output folder must have contents!");
      
        final IHierarchicalDataFile hFile = HierarchicalDataFactory.getReader(h5.getAbsolutePath());
        try {
        	final List<String> names = hFile.getDatasetNames(IHierarchicalDataFile.TEXT);
        	if (names.size()!=noResults) throw new Exception("Unexpected data sets from test file '"+afile+"' in hdf5 file '"+h5.getName()+"' they were:  "+names);
                    	
        } finally {
        	hFile.close();
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
