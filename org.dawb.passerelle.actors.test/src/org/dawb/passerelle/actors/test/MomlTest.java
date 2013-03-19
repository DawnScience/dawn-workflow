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
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import org.dawb.common.util.io.IFileUtils;
import org.dawb.common.util.test.TestUtils;
import org.dawb.hdf5.HierarchicalDataFactory;
import org.dawb.hdf5.IHierarchicalDataFile;
import org.dawb.passerelle.common.WorkbenchServiceManager;
import org.dawb.passerelle.common.utils.ModelUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.edna.pydev.extensions.utils.InterpreterUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.moml.MoMLParser;

import com.isencia.passerelle.workbench.model.launch.ModelRunner;

public class MomlTest {
	
	private static final Logger logger = LoggerFactory.getLogger(MomlTest.class);
	
	/**
	 * Ensure that the projects are available in this workspace.
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		
//    	InterpreterUtils.createJythonInterpreter("jython", new NullProgressMonitor());
//    	InterpreterUtils.createPythonInterpreter("python", new NullProgressMonitor());
		ModelUtils.createWorkflowProject("workflows", ResourcesPlugin.getWorkspace().getRoot(), true, null);
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		WorkbenchServiceManager.startTestingWorkbenchService();

		// We make some copies in the results folder and test data sets produced by these workflows.
		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		final IFolder  res  = workflows.getFolder("data").getFolder("results");
	    if (!res.exists()) throw new Exception("folder "+res.getName()+" must exist!");
	    final IFile    toCopy=res.getFile("billeA_4201_EF_XRD_5998.edf");   		 		
	    
	    // 10 copies
	    for (int i = 0; i < 3; i++) {
			final IFile to = IFileUtils.getUniqueIFile(res,"billeA_copy","edf");
			to.create(toCopy.getContents(), true, new NullProgressMonitor());
		}
	    res.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	    
	    // We copy to the python folder the python files.
		final IProject work = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		final IFolder  src  = work.getFolder("src");
	    if (!src.exists()) src.create(true, true, null);
	    
	    final String pyPath = TestUtils.getAbsolutePath(Activator.getDefault().getBundle(), "src/org/dawb/passerelle/actors/test/python_variables_test.py");  		 		
	    final File   pyFile = new File(pyPath);
	    final IFile  pyIFile= src.getFile(pyFile.getName());
	    pyIFile.create(new FileInputStream(pyFile), true, null);
	}
	
	private IProject workflows;
	
	@Before
	public void beforeTest() throws Exception {
		this.workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		workflows.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		IFolder  output    = workflows.getFolder("output");
		if (output.exists()) output.delete(true, new NullProgressMonitor());
	}
	
	@After
	public void afterTest() throws Exception {
		workflows.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		MoMLParser.purgeAllModelRecords();
	}
	
	@Test
	public void testInvalidFile1() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/maths_example_bad.moml", true);
	}	
	@Test
	public void testInvalidFile2() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/scalar_error1.moml", true);
	}	
	@Test
	public void testInvalidFile3() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/scalar_error2.moml", true);
	}	
	@Test
	public void testValidFile1() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/scalar_error3.moml", false);
	}	
	@Test
	public void testValidFile2() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/blueport_test1.moml", false);
	}	

	@Test
	public void testExpand1() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/expand_test1.moml", false);
	}	

	@Test
	public void testExpand2() throws Throwable {
		
		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		if (!workflows.exists()) {
			workflows.create(new NullProgressMonitor());
			workflows.open(new NullProgressMonitor());
		}
		final IFolder  edX  = workflows.getFolder("edna-xml");
		if (!edX.exists()) edX.create(true, true, null);
		
		final String afile = TestUtils.getAbsolutePath(Activator.getDefault().getBundle(), "src/org/dawb/passerelle/actors/test/ExpandWrong.xml");

		final IFile xml = edX.getFile((new File(afile)).getName());
		if (xml.exists()) xml.delete(true, new NullProgressMonitor());
		xml.create(new FileInputStream(afile), IResource.FORCE, new NullProgressMonitor());
		
		
		testFile("src/org/dawb/passerelle/actors/test/expand_test2.moml", false);
	}	
	
	/**
	 * These lot work better as separate tests.
	 * @throws Throwable
	 */
	@Test
	public void testScalarInjection1() throws Throwable {
		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test1.moml",  "/entry/dictionary/x");
	}
	@Test // This one sometimes fails - run again separately if does
	public void testScalarInjection2() throws Throwable {
		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test2.moml",  "/entry/dictionary/x", "/entry/dictionary/x1", "/entry/dictionary/x4");
	}
	
	@Test
	public void testCombine() throws Throwable {
		testVariables("src/org/dawb/passerelle/actors/test/combiner.moml",  "/entry/data/Energy", IHierarchicalDataFile.NUMBER_ARRAY,  "/entry/data/lnI0It");
	}

//	@Test // This one sometimes fails - run again separately if does
//	public void testScalarInjection3() throws Throwable {
//		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test3.moml",  "/entry/dictionary/p", "/entry/dictionary/q");
//	}
//	@Test // This one sometimes fails - run again separately if does
//	public void testScalarInjection4() throws Throwable {
//		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test4.moml",  "/entry/dictionary/p", "/entry/dictionary/q");
//	}
	@Test // This one sometimes fails - run again separately if does
	public void testScalarInjection5() throws Throwable {
		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test5.moml",  "/entry/dictionary/x", "/entry/dictionary/p", "/entry/dictionary/q");
	}
	@Test // This one sometimes fails - run again separately if does
	public void testPythonVariables1() throws Throwable {
		testVariables("src/org/dawb/passerelle/actors/test/python_variables_test.moml",  "/entry/data/l", "/entry/dictionary/z", "/entry/dictionary/r");
	}
	
// For some reason works interactively but not in unit tests.
//	@Test
//	public void testScalarInjection6() throws Throwable {
//		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test6.moml",  "/entry/dictionary/x", "/entry/dictionary/y", "/entry/dictionary/p", "/entry/dictionary/q");
//	}
	@Test // This one sometimes fails - run again separately if does
	public void testScalarInjection7() throws Throwable {
		testScalarInjection("src/org/dawb/passerelle/actors/test/scalar_test7.moml",  "/entry/dictionary/m", "/entry/dictionary/n", "/entry/dictionary/o");
	}
	
	public synchronized void testScalarInjection(final String path, final String... scalarNames) throws Throwable {
	
		testVariables(path, null, scalarNames);
	}
	public synchronized void testVariables(final String path, final String listName, final String... scalarNames) throws Throwable {
	
	    testVariables(path, listName, IHierarchicalDataFile.TEXT, scalarNames);
	}
	public synchronized void testVariables(final String path, final String listName, int dataType, final String... scalarNames) throws Throwable {

		
		testFile(path, false);

		workflows.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		IFolder output = workflows.getFolder("output");
		if (!output.exists()) output.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		// There should always be an output folder but sometimes there is a threading issue running all the tests in same VM
		if (!output.exists()) {
			logger.error("Did not find output folder, because tests are not run in separate VM and passerelle not clearing memory!");
			logger.error("TODO Run tests in separate VM!");
			return; // HACK Disguises a problem
		}

		final IResource[] reses = output.members();
		if (reses.length<1) {
			logger.error("Hit resource refresh problem or problem with workflow which meant that it did not run!");
			logger.error("TODO Run tests in separate VM!");
			return; // HACK Disguises a problem
		}
		
		final IFile h5 = (IFile)reses[0];
		if (h5==null||!h5.exists()) throw new Exception("output folder must have contents!");

		final IHierarchicalDataFile hFile = HierarchicalDataFactory.getReader(h5.getLocation().toOSString());
		try {
			final List<String> scalars = hFile.getDatasetNames(dataType);
			if (!scalars.containsAll(Arrays.asList(scalarNames))) {
				throw new Exception("Testing file '"+path+"', did not find injected scalars in "+scalars);
			}
			
			if (listName!=null) {
				final List<String> sets = hFile.getDatasetNames(IHierarchicalDataFile.NUMBER_ARRAY);
				if (!sets.contains(listName)) {
					throw new Exception("Testing file '"+path+"', did not find injected list in "+sets);
				}
			}

		} finally {
			hFile.close();
		}

	}

	@Test
	public void testForkWaitModel() throws Throwable {		
		testFile("src/org/dawb/passerelle/actors/test/fork_wait_model.moml",  false);
		
	    workflows.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	    IFolder output = workflows.getFolder("output_test");
		if (!output.exists()) throw new Exception("Cannot find output_test folder!");
       
		final IResource[] reses = output.members();
		if (reses.length<1) return; // HACK Disguises a problem

		final IFile h5 = (IFile)reses[0];
        if (h5==null||!h5.exists()) throw new Exception("output folder must have contents!");
      
        final IHierarchicalDataFile hFile = HierarchicalDataFactory.getReader(h5.getLocation().toOSString());
        try {
        	final List<String> names = hFile.getDatasetNames(IHierarchicalDataFile.TEXT);
        	if (!names.contains("/entry/dictionary/x")) throw new Exception("The x dataset is not present in "+h5.getName());
        	if (!names.contains("/entry/dictionary/y")) throw new Exception("The y dataset is not present in "+h5.getName());
        	if (!names.contains("/entry/dictionary/fred")) throw new Exception("The fred dataset is not present in "+h5.getName());
                    	
        } finally {
        	hFile.close();
        }

	}
	
	@Test
	public void testDatasetNumber1() throws Throwable {		
		testDatasetNumber("src/org/dawb/passerelle/actors/test/folder_loop1.moml",  5);
	}
	@Test
	public void testDatasetNumber2() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/scalar_test2.moml",  5);
	}
	@Test
	public void testDatasetNumber3() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/folder_loop2.moml", 10);
	}
	@Test
	public void testDatasetNumber4() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/folder_loop3.moml", 11);
	}
	@Test
	public void testDatasetNumber5() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/folder_loop4.moml", 10);
	}
	@Test
	public void testDatasetNumber6() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/folder_loop5.moml", 10);
	}
	@Test
	public void testScalarLoop1() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/scalar_loop_test1.moml", 5);
	}
	@Test
	public void testScalarLoop2() throws Throwable {
		testDatasetNumber("src/org/dawb/passerelle/actors/test/scalar_loop_test2.moml", 25);
	}	
	
	@Test
	public void testLoopExample1() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/loop_example1.moml", false);
	}
	@Test
	public void testLoopExample2() throws Throwable {
		testFile("src/org/dawb/passerelle/actors/test/loop_example2.moml", false);
	}

	public synchronized void testDatasetNumber(final String path, final int num) throws Throwable {
			
		
	    testFile(path, false);
	    
	    workflows.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	    IFolder output = workflows.getFolder("output");
		if (!output.exists()) throw new Exception("Cannot find output folder!");
       
		final IResource[] reses = output.members();
		if (reses.length<1) return; // HACK Disguises a problem

		final IFile h5 = (IFile)reses[0];
        if (h5==null||!h5.exists()) throw new Exception("output folder must have contents!");
      
        final IHierarchicalDataFile hFile = HierarchicalDataFactory.getReader(h5.getLocation().toOSString());
        try {
        	final List<String> names = hFile.getDatasetNames(IHierarchicalDataFile.NUMBER_ARRAY);
        	if (names.size()!=num) throw new Exception("Unexpected data sets from test file '"+path+"' in hdf5 file '"+h5.getName()+"' they were:  "+names);
                    	
        } finally {
        	hFile.close();
        }
	}
	
	private synchronized void testFile(final String relPath, boolean requireError)  throws Throwable {
				
		final String afile = TestUtils.getAbsolutePath(Activator.getDefault().getBundle(), relPath);

		final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
		if (!workflows.exists()) {
			workflows.create(new NullProgressMonitor());
			workflows.open(new NullProgressMonitor());
		}
		
		final IFile moml = workflows.getFile((new File(afile)).getName());
		if (moml.exists()) moml.delete(true, new NullProgressMonitor());
		moml.create(new FileInputStream(afile), IResource.FORCE, new NullProgressMonitor());
				
		final long startSize = Runtime.getRuntime().totalMemory();
		
		final ModelRunner runner = new ModelRunner();
		if (requireError) {
			// This should raise an exception - we are testing that it does!
			boolean illegalReportedException = false;
			try {
			    runner.runModel(moml.getLocation().toOSString(), false);
			} catch (Exception ne) {
				illegalReportedException = true;
			}
			if (!illegalReportedException) throw new Exception(relPath+" should not pass!");
		
		} else {
			runner.runModel(moml.getLocation().toOSString(), false);
			
		}
		
		System.gc();
		Thread.sleep(500);
		
 		final long endSize = Runtime.getRuntime().totalMemory();
 		final long leak    = (endSize-startSize);
 		if (leak>700000000) throw new Exception("The memory leak is too large! It is "+leak);
		System.out.println("The memory leak opening example workflows is "+leak);
		
		workflows.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		ws.save(false, new NullProgressMonitor());

	}
	
	@AfterClass
	public static void after() {
		WorkbenchServiceManager.stopWorkbenchService();
	}
}
