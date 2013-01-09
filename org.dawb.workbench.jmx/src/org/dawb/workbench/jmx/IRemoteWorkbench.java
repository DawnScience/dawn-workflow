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

import java.util.Map;

/**
 * Interface used to talk to the workbench.
 * 
 * This interface is passed into the service. This allows the JMX and
 * its implementation to be disconnected meaning anywhere can import and
 * use this jmx plugin without having dependencies. When the serice is started,
 * it must be provided with an implementation of this remote workbench.
 * 
 * NOTE in 0.9 Tigermoth, methods here will be changed to use objects
 * if they have three arguments or more.
 * 
 * @author gerring
 *
 */
public interface IRemoteWorkbench {

	/**
	 * Called in RCP mode when the execution of the model 
	 * has started. Works in debug and run.
	 **/
	public void executionStarted();
	
	/**
	 * Called in RCP mode when the execution of the model 
	 * has completed. Works in debug and run.
	 * 
	 * Not required for the external service such as Tango
	 * and the model process terminates with a code.
	 * 
	 * @param returnCode - zero if ok
	 */
	public void executionTerminated(final int returnCode);
	
	/**
	 * When called, this method expects a file to be opened
	 * and shown to the user, for instance an image.
	 * 
	 * The implementation of which part is shown to the user
	 * is left to you. In the case of an eclispe workbench the 
	 * registered editor part is normally used.
	 * 
	 * @param fullPath
	 * @return
	 */
	public boolean openFile(final String fullPath);
	
	/**
	 * Called when the workflow would like the user to 
	 * be show a part which monitors images being read into
	 * a directory.
	 * 
	 * @param fullPath
	 * @return
	 */
	public boolean monitorDirectory(final String fullPath, final boolean startMonitoring);
	
	/**
	 * This method notifies that a certain project and path have changed in
	 * the workspace. After being called, it expects any view of this file
	 * to be updated in the UI. For instance the output files of an EDNA actor.
	 * 
	 * @param projectName
	 * @param resourcePath
	 * @return
	 */
	public boolean refresh(final String projectName, final String resourcePath);
	
	
	/**
	 * This method should show a dialog and return after the user presses ok.
	 * If the method returns true workflow will continue, if false it
	 * will stop with an error message.
	 * 
	 * 
	 * @param title
	 * @param message
	 * @param type
	 * @return
	 */
	public boolean showMessage(final String title, final String message, final int type);


	/**
	 * Defines an exception encountered in the workflow. If the client is an eclipse workbench,
	 * this error is logged in the error log view. Otherwise it should be added to the local 
	 * log file.
	 * 
	 * @param pluginId
	 * @param message
	 * @param throwable
	 */
	public void logStatus(final String pluginId, final String message, final Throwable throwable);
	
	
    /**
     * Call to ask the user to check and edit editScalarValues. The method will 
     *  **block** until they have finished interacting with the custom UI. Once
     *  it returns, the workflow will continue again. If intend to create an
     *  implementation 
     * 
     * @param parName  - part name input should appear with. Just a name to be used by the UI
     * @param isDialog - if true workflow would like a modal dialog used, otherwise editor part used.
     * @param configuration - can be null, just used 
     * @param scalarValues  - can be null
     * @param silent        - true or false, if true, the user will not see the user input form,
     *                        and the default values will be used directly.
     * 
     * (But both cannot be null!)
     *
     * @return
     * @throws Exception
     */
	public Map<String,String> createUserInput(final UserInputBean bean) throws Exception;
	
    /**
     * Plots data in the PlotInputBean and returns the plot data, any regions, the plot settings,
     * tool output.
     * 
     * @param bean
     * @return plot output
     * @throws Exception
     */
	public UserPlotBean createPlotInput(final UserPlotBean bean) throws Exception;


	/**
	 * This method allows certain actors to be selected if the editor for the corresponding file is open.
	 * 
	 * Once the actor has finished executing, it must call again to unselect. So the implemention should
	 * be:
	 * try {
	 *    x.setActorSelected(... true ..);
	 *   
	 *   ... something
	 * } finally {
	 *    x.setActorSelected(... false ..);
	 * }
	 * 
	 * @param resourcePath
	 * @param actorName
	 * @param isSelected
	 * @return true if the actor is found, false otherwise
	 */
	public boolean setActorSelected(final String  resourcePath,
			                        final String  actorName,
			                        final boolean isSelected,
			                        final int     colorCode) throws Exception;
	
	/**
	 * Called to set mock values of motors from the workflow. The workflow does not
	 * keep mock values. These are stored in the IRemoteWorkbench implementation so
	 * that the UI can show real vaules.
	 * 
	 * @param isMockMode
	 */
	public void setMockMotorValue(final String motorName, final Object value);

	/**
	 * Called to get mock values of motors from the workbench. The workflow does not
	 * keep mock values. These are stored in the IRemoteWorkbench implementation so
	 * that the UI can show real vaules.
     *
	 * @param motorName
	 * @return an object which is often a Number
	 */
	public Object getMockMotorValue(final String motorName);
	
	/**
	 * Called to notify of command results in mock mode. This fires listeners inside the
	 * workbench which make be waiting for mock motor events to fire so that the UI can
	 * be updated.
	 * 
	 * @param motorName
	 * @param value
	 */
	public void notifyMockCommand(final String motorName, final String message, final String cmd);

}
