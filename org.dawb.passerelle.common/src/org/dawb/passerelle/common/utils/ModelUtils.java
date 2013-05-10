/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dawb.common.util.eclipse.BundleUtils;
import org.dawb.common.util.io.FileUtils;
import org.dawb.passerelle.common.project.PasserelleNature;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.edna.pydev.extensions.utils.PydevProjectUtils;
import org.python.pydev.core.IPythonNature;

/**
 *   ModelUtils
 *
 *   @author gerring
 *   @date Jul 19, 2010
 *   @project org.edna.passerelle.common
 **/
public class ModelUtils {


	/**
	 * 
	 * @param string
	 * @param root
	 * @param mon
	 */
	public static IProject createWorkflowProject(String projectName, IWorkspaceRoot root, boolean examples, IProgressMonitor mon) throws Exception {
		
		if (root.getProject(projectName).exists()) return root.getProject(projectName);
		
		final IProject workflows = ModelUtils.createPasserelleProject(projectName, root, examples, mon);
		
		// We add a nature which will use the 'jython' interpreter created automatically.
		// This allows jython scripting to run in workflow nodes.
		//final IPythonNature jython = PydevProjectUtils.addJythonNature(workflows, "jython", mon);
		PydevProjectUtils.createSrcAndExample(workflows, null, "helloworld", null, mon);
		
		if (examples) {
			final IFolder srcInProject = workflows.getFolder("src");
			final File    examplesDir  = BundleUtils.getBundleLocation("org.dawb.workbench.examples");
	              File    extraDir     = new File(examplesDir, "python"); // Where the data comes from
	        if (extraDir.exists()) {
	        	for (File d : extraDir.listFiles()) {
	        		if (!d.exists())  continue;
	        		if (!d.canRead()) continue;
	        		if (!d.isFile())  continue;
	        		
	        		final IFile file = srcInProject.getFile(d.getName());
	        		file.create(new FileInputStream(d), false, mon);
	        	}
	   
	        }
	        
	        extraDir   = new File(examplesDir, "configuration"); // Where the data comes from
	        if (extraDir.exists()) {
	        	final IFolder confInProject = workflows.getFolder("configuration");
	        	if (!confInProject.exists()) confInProject.create(true, true, mon);
	        	for (File d : extraDir.listFiles()) {
	        		if (!d.exists())  continue;
	        		if (!d.canRead()) continue;
	        		if (!d.isFile())  continue;

	        		final IFile file = confInProject.getFile(d.getName());
	        		file.create(new FileInputStream(d), false, mon);
	        	}
	        }	
	        
	        
	        final IFolder dataFolder = workflows.getFolder("data");
	        dataFolder.create(true, true, mon);
			
			// We copy all the data from examples here.
			// Use bundle as works even in debug mode.
	        final File dataDir     = new File(examplesDir, "data");
	        if (dataDir.exists()) {
	        	FileUtils.recursiveCopy(dataDir, new File(dataFolder.getLocation().toOSString()));
	        }
		}
		
		workflows.refreshLocal(IResource.DEPTH_INFINITE, mon);
		
		return workflows;
	}

	/**
	 * 
	 * @param container
	 * @param name
	 * @param monitor
	 * @throws CoreException 
	 */
	public static IFile createFolderMonitorWorkflow(final IContainer container, final String name, final File toMonitor, final IProgressMonitor monitor) throws CoreException {
		
		final IFile file = container.getFile(new Path(name));
		file.create(openFolderMonitorStream(file, toMonitor), true, monitor);
		return file;
	}
	
	/**
	 * Some code copied from Passerelle (theirs private method)
	 * @return
	 */
	private static InputStream openFolderMonitorStream(final IFile modelFile, final File toMonitor) {
		
		String contents =		
		"<?xml version=\"1.0\" standalone=\"no\"?>\n" +
		"<!DOCTYPE entity PUBLIC \"-//UC Berkeley//DTD MoML 1//EN\"\n" +
		"    \"http://ptolemy.eecs.berkeley.edu/xml/dtd/MoML_1.dtd\">\n" +
		"<entity name=\""+modelFile.getName().substring(0, modelFile.getName().indexOf('.'))+"\" class=\"ptolemy.actor.TypedCompositeActor\" source=\""+modelFile.getLocation().toOSString()+"\">\n" +
		"    <property name=\"_createdBy\" class=\"ptolemy.kernel.attributes.VersionAttribute\" value=\"7.0.1\">\n" +
		"    </property>\n" +
		"    <property name=\"_workbenchVersion\" class=\"ptolemy.kernel.attributes.VersionAttribute\" value=\"1.0.0.qualifier\">\n" +
		"    </property>\n" +
		"    <property name=\"Director\" class=\"com.isencia.passerelle.domain.cap.Director\">\n" +
		"       <property name=\"_location\" class=\"ptolemy.kernel.util.Location\" value=\"{20, 20}\">\n" +
		"        </property>\n" +
		"    </property>\n" +
		"    <entity name=\"Folder Monitor\" class=\"org.dawb.passerelle.actors.data.FolderMonitorSource\">\n" +
		"        <property name=\"Receiver Q Capacity (-1)\" class=\"ptolemy.data.expr.Parameter\" value=\"-1\">\n" +
		"        </property>\n" +
		"        <property name=\"Receiver Q warning size (-1)\" class=\"ptolemy.data.expr.Parameter\" value=\"-1\">\n" +
		"        </property>\n" +
		"        <property name=\"_icon\" class=\"com.isencia.passerelle.actor.gui.EditorIcon\">\n" +
		"        </property>\n" +
		"        <property name=\"Folder\" class=\"com.isencia.passerelle.util.ptolemy.ResourceParameter\" value=\""+toMonitor.getAbsolutePath()+"\">\n" +
		"        </property>\n" +
		"        <property name=\"Checking Frequency\" class=\"ptolemy.data.expr.Parameter\" value=\"100\">\n" +
		"        </property>\n" +
		"        <property name=\"Inactive After\" class=\"ptolemy.data.expr.Parameter\" value=\"-1\">\n" +
		"        </property>\n" +
		"        <property name=\"Relative Path\" class=\"ptolemy.data.expr.Parameter\" value=\"false\">\n" +
		"        </property>\n" +
		"        <property name=\"File Filter\" class=\"com.isencia.passerelle.util.ptolemy.RegularExpressionParameter\" value=\"\">\n" +
		"        </property>\n" +
		"        <property name=\"_location\" class=\"ptolemy.kernel.util.Location\" value=\"{20.0, 179.0}\">\n" +
		"        </property>\n" +
		"        <port name=\"requestFinish\" class=\"com.isencia.passerelle.core.ControlPort\">\n" +
		"            <property name=\"input\"/>\n" +
		"            <property name=\"multiport\"/>\n" +
		"            <property name=\"control\" class=\"ptolemy.kernel.util.StringAttribute\">\n" +
		"            </property>\n" +
		"        </port>\n" +
		"        <port name=\"error\" class=\"com.isencia.passerelle.core.ErrorPort\">\n" +
		"            <property name=\"output\"/>\n" +
		"            <property name=\"multiport\"/>\n" +
		"            <property name=\"error\" class=\"ptolemy.kernel.util.StringAttribute\">\n" +
		"            </property>\n" +
		"        </port>\n" +
		"        <port name=\"hasFired\" class=\"com.isencia.passerelle.core.ControlPort\">\n" +
		"            <property name=\"output\"/>\n" +
		"            <property name=\"multiport\"/>\n" +
		"            <property name=\"control\" class=\"ptolemy.kernel.util.StringAttribute\">\n" +
		"            </property>\n" +
		"        </port>\n" +
		"        <port name=\"hasFinished\" class=\"com.isencia.passerelle.core.ControlPort\">\n" +
		"            <property name=\"output\"/>\n" +
		"            <property name=\"multiport\"/>\n" +
		"            <property name=\"control\" class=\"ptolemy.kernel.util.StringAttribute\">\n" +
		"            </property>\n" +
		"        </port>\n" +
		"        <port name=\"output\" class=\"com.isencia.passerelle.core.Port\">\n" +
		"            <property name=\"output\"/>\n" +
		"            <property name=\"multiport\"/>\n" +
		"        </port>\n" +
		"        <port name=\"trigger\" class=\"com.isencia.passerelle.core.Port\">\n" +
		"            <property name=\"input\"/>\n" +
		"            <property name=\"multiport\"/>\n" +
		"        </port>\n" +
		"    </entity>\n" +
		"</entity>";
		return new ByteArrayInputStream(contents.getBytes());
	}


	/**
	 * 
	 * @param container
	 * @param name
	 * @param monitor
	 * @throws CoreException 
	 */
	public static void createEmptyWorkflow(final IContainer container, final String name, final IProgressMonitor monitor) throws CoreException {
		
		final IFile file = container.getFile(new Path(name));
		file.create(openContentStream(), true, monitor);
	}
	
	/**
	 * Some code copied from Passerelle (theirs private method)
	 * @return
	 */
	private static InputStream openContentStream() {
		String contents =
			"<?xml version=\"1.0\" standalone=\"no\"?> \r\n" + 
			"<!DOCTYPE entity PUBLIC \"-//UC Berkeley//DTD MoML 1//EN\" \"http://ptolemy.eecs.berkeley.edu/xml/dtd/MoML_1.dtd\"> \r\n" +
            "<entity name=\"newModel\" class=\"ptolemy.actor.TypedCompositeActor\"> \r\n" +
            "   <property name=\"_createdBy\" class=\"ptolemy.kernel.attributes.VersionAttribute\" value=\"7.0.1\" /> \r\n" +
            "   <property name=\"_workbenchVersion\" class=\"ptolemy.kernel.attributes.VersionAttribute\" value=\""+System.getProperty("dawb.workbench.version")+"\" /> \r\n" +
            "   <property name=\"Director\" class=\"com.isencia.passerelle.domain.cap.Director\" > \r\n" +
        	"      <property name=\"_location\" class=\"ptolemy.kernel.util.Location\" value=\"{20, 20}\" /> \r\n" +
            "   </property> \r\n" +
        	"</entity>";
		return new ByteArrayInputStream(contents.getBytes());
	}

	public static IProject createPasserelleProject(final String           name,
			                                       final IWorkspaceRoot   root,
			                                       final boolean          createExamples,
			                                       final IProgressMonitor mon) throws Exception {
		// Make projects workflows and data.
		final IProject workflows = root.getProject(name);
		workflows.create(mon);
		workflows.open(mon);
			
		ModelUtils.createEmptyWorkflow(workflows, "empty_workflow.moml", mon);
		
		if (createExamples) {
			final IFolder examplesInProject = workflows.getFolder("examples");
			examplesInProject.create(true, true, mon);
	        final File examplesDir  = BundleUtils.getBundleLocation("org.dawb.workbench.examples");
	        final File workflowsDir = new File(examplesDir, "workflows"); // Where the data comes from
	        if (workflowsDir.exists()) {
	        	for (File d : workflowsDir.listFiles()) {
	        		if (!d.exists())  continue;
	        		if (!d.canRead()) continue;
	        		if (!d.isFile())  continue;
	        		
	        		final IFile file = examplesInProject.getFile(d.getName());
	        		file.create(new FileInputStream(d), false, mon);
	        	}
	   
	        }
	        
	        // Separate example folder for EDNA workflows.
	        // Edna workflows are not currently supported in the SDA product. Therefore
	        // we only do this in dawb.
	        final File ednaDataDir     = new File(examplesDir, "workflows-edna"); // Where the data comes from
	        if (ednaDataDir.exists()) {
				final IFolder ednaExamplesInProject = workflows.getFolder("examples-edna");
				ednaExamplesInProject.create(true, true, mon);
		        if (ednaDataDir.exists()) {
		        	for (File d : ednaDataDir.listFiles()) {
		        		if (!d.exists())  continue;
		        		if (!d.canRead()) continue;
		        		if (!d.isFile())  continue;
		        		
		        		final IFile file = ednaExamplesInProject.getFile(d.getName());
		        		file.create(new FileInputStream(d), false, mon);
		        	}
		   
		        }
	        }
	        
	        // Separate example folder for ICAT workflows.
	        final File icatDataDir     = new File(examplesDir, "workflows-icat"); // Where the data comes from
	        if (icatDataDir.exists()) {
				final IFolder icatExamplesInProject = workflows.getFolder("examples-icat");
				icatExamplesInProject.create(true, true, mon);
		        if (icatDataDir.exists()) {
		        	for (File d : icatDataDir.listFiles()) {
		        		if (!d.exists())  continue;
		        		if (!d.canRead()) continue;
		        		if (!d.isFile())  continue;
		        		
		        		final IFile file = icatExamplesInProject.getFile(d.getName());
		        		file.create(new FileInputStream(d), false, mon);
		        	}
		   
		        }
	        }
		}

		ModelUtils.addPasserelleNature(workflows, mon);
		
		return workflows;
	}
	
	/**
	 * 
	 * @param workflows
	 * @param mon
	 * @throws CoreException 
	 */
	private static void addPasserelleNature(final IProject workflows,
			                                final IProgressMonitor mon) throws CoreException {
		
		
		IProjectDescription description = workflows.getDescription();
		description.setNatureIds(new String[]{PasserelleNature.ID});
		workflows.setDescription(description, mon);
		
	}

}
