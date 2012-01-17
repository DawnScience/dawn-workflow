/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx;

public interface IRemoteServiceProvider {

	
	/**
	 * Implement to return your remote workbench instance. For OSGI this is
	 * null and the bench is loaded by OSGI but for Tango interface this is simply 
	 * a new IRemoteWorkbench instance.
	 */
	public IRemoteWorkbench getRemoteWorkbench() throws Exception ;
	
	/**
	 * Implement this method to return the start port, usually 21701. In the case of the eclipse
	 * workbench, this value is the port returned by a user interface property editable by the user.
	 * 
	 * @return
	 */
	public int getStartPort();

	/**
	 * The absolute path to the workspace where the workflow should be run within.
	 * 
	 * Should be null if the provider is run from within a RCP UI.
	 * 
	 * Path may not contain spaces.
	 * 
	 * @return
	 */
	public String getWorkspacePath();

	/**
	 * The absolute path to where the model, "moml" file is location.
	 * 
	 * Should be null if the provider is run from within a RCP UI.
	 * 
	 * Path may not contain spaces.
	 * 
	 * @return
	 */
	public String getModelPath();

	/**
	 * Implement this method to return the absolute path to the installation the eclipse application
	 * executable, for instance dawn or sda.
	 * 
	 * The system adds the arguments -data getWorkspacePath() and -Dmodel=getModelPath() to this
	 * command in order to run the workbench. 
	 * 
	 * Path may not contain spaces.
	 */
	public String getInstallationPath();
	
	/**
	 * Determines if the service should be terminated or not
	 * 
	 */
	public boolean getServiceTerminate();

	/**
	 * Determines if the workflow should be started in mock mode or not
	 * 
	 */
	public boolean getTangoSpecMockMode();

}
