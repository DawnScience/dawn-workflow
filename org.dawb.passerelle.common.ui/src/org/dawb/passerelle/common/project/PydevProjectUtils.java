/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.project;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 *   PydevProjectUtils
 *
 *   @author gerring
 *   @date Jul 28, 2010
 *   @project org.edna.pydev.extensions
 **/
public class PydevProjectUtils {

	public static IProject createPydevProject(final String           name, 
			                                  final IWorkspaceRoot   root,
			                                  final boolean          requireSrcAndExample,
			                                  final IProgressMonitor mon) throws Exception {

		if (root.getProject(name).exists()) return root.getProject(name);
       
		
		// get a project handle
		final IProject project = root.getProject(name);
		project.create(mon);
		project.open(mon);
		
		final IPythonNature nature = addPythonNature(project, mon);
		root.refreshLocal(IResource.DEPTH_INFINITE, mon);
		
		if (requireSrcAndExample) PydevProjectUtils.createSrcAndExample(project, nature, "example", null, mon);

		project.refreshLocal(IResource.DEPTH_INFINITE, mon);
		
		// We leave building off by default	
		PydevPlugin.getDefault().getPreferenceStore().setDefault(PyDevBuilderPrefPage.USE_PYDEV_BUILDERS, false);
			
		return project;
		
	}

	public static IPythonNature addPythonNature(final IProject project, final IProgressMonitor mon) throws CoreException {
		// We add a nature which will use the 'python' interpreter created automatically or if no python is installed,
		// we use the jython interpreter.
		IPythonNature nature;
		
		if (isPythonNature("python", mon)) {
			nature = PythonNature.addNature(project, mon, PythonNature.PYTHON_VERSION_2_6, null, null, "python", null);
		} else {
			// One that should work on any platform.
			nature = PythonNature.addNature(project, mon, PythonNature.JYTHON_VERSION_2_5, null, null, "jython", null);
		}
		return nature;
	}

	private static boolean isPythonNature(final String           name, 
			                              final IProgressMonitor mon) {
		try {
			return PydevPlugin.getPythonInterpreterManager().getInterpreterInfo(name, mon)!=null;
		} catch (Exception ne) {
			return false;
		}
	}

	/**
	 * 
	 * @param project
	 * @param nature, may be null
	 * @param name
	 * @param stream
	 * @param mon
	 * @throws Exception
	 */
	public static void createSrcAndExample(final IProject project, 
			                               final IPythonNature nature, 
			                               final String name,
			                                     InputStream stream,
			                               final IProgressMonitor mon) throws Exception {
		

		IFolder folder = project.getFolder("src");
        folder.create(true, true, mon);
    
        if (nature!=null) nature.getPythonPathNature().setProjectSourcePath(folder.getFullPath().toString());
        
        IFile file = folder.getFile(name+".py");  
        // Hope Java does not choke at this point....
        if (stream==null) stream = getHelloWorldPython();
        file.create(stream, true, mon);
        
        file = folder.getFile(name+".launch");  
        
        if (nature!=null) {
	        final boolean isPython = nature.getInterpreterType()==IPythonNature.INTERPRETER_TYPE_PYTHON;
	        file.create(getLaunchConfig(project.getName(), name, isPython), true, mon);
	
	        nature.rebuildPath();
        }
        
        project.refreshLocal(IResource.DEPTH_INFINITE, mon);
	}

	private static InputStream getHelloWorldPython() {
		String contents = "# An exmple python script\r\n" +
		                  "# Please chose run or debug to run the script with the \r\n" + 
		                  "# python interpreter found automatically by the workbench. \r\n" + 
		                  "# For instance go to 'Run' and choose 'example'. \r\n" + 
		                  "\r\n" + 
		                  "def hello():\r\n" +
		                  "    print \"Hello World!\"\r\n"+
		                  "\r\n" + 
		                  "\r\n" + 
                          "hello()";        
	
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private static InputStream getLaunchConfig(final String projectName, final String pyFileName, final boolean isPython) throws UnsupportedEncodingException {
		StringBuilder contents = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n");
		if (isPython) {
			contents.append("<launchConfiguration type=\"org.python.pydev.debug.regularLaunchConfigurationType\">\r\n");
			
		} else {
			contents.append("<launchConfiguration type=\"org.edna.pydev.extensions.jythonInSameVM\">\r\n");
		}
		 
		contents.append("<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\">\r\n" );
		contents.append("<listEntry value=\"/"+projectName+"/src/"+pyFileName+"\"/>\r\n");
		contents.append("</listAttribute>\r\n");
		contents.append("<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\">\r\n");
		contents.append("<listEntry value=\"1\"/>\r\n");
		contents.append("</listAttribute>\r\n");
		contents.append("<listAttribute key=\"org.eclipse.debug.ui.favoriteGroups\">\r\n");
		contents.append("<listEntry value=\"org.eclipse.debug.ui.launchGroup.debug\"/>\r\n");
		contents.append("</listAttribute>\r\n");
		contents.append("<stringAttribute key=\"org.eclipse.ui.externaltools.ATTR_LOCATION\" value=\"${workspace_loc:"+projectName+"}/src/"+pyFileName+".py\"/>\r\n");
		contents.append("<stringAttribute key=\"org.eclipse.ui.externaltools.ATTR_OTHER_WORKING_DIRECTORY\" value=\"\"/>\r\n");
		contents.append("<stringAttribute key=\"org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS\" value=\"\"/>\r\n");
		final String interpreter = isPython ? "python" : "jython";
		contents.append("<stringAttribute key=\"org.python.pydev.debug.ATTR_INTERPRETER\" value=\""+interpreter+"\"/>\r\n");
		contents.append("<stringAttribute key=\"org.python.pydev.debug.ATTR_PROJECT\" value=\""+projectName+"\"/>\r\n");
		contents.append("</launchConfiguration>\r\n");
		
		return new ByteArrayInputStream(contents.toString().getBytes("UTF-8"));
	}

	public static IPythonNature addJythonNature(final IProject         workflows, 
					                            final String           name, 
					                            final IProgressMonitor mon) throws CoreException {

		final IPythonNature nature = PythonNature.addNature(workflows, mon, PythonNature.JYTHON_VERSION_2_5, null, null, name, null);

		nature.rebuildPath();

		return nature;
	}
}
