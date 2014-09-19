/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.service;

/**
 * NOTE: The service implementation returned is only working fully on linux
 *       at the moment.
 * 
 * @author gerring
 *
 */
public interface IWorkflowService {

	/**
	 * Runs the workflow and returns the exit value once it has finished.
	 * 
	 * @param prov
	 */
	public int run() throws Exception;
	
   /**
	 * Call this method to start the workflow. It will throw an exception
	 * if the workflow is already running and the system allows only
	 * one to be running at a time. It returns the process the workflow 
	 * service is using.
	 * 
	 * NOTE: This method is non-blocking and will start the workflow 
	 * and return.
	 * 
	 * @param prov
	 */
	public Process start() throws Exception;
	
	/**
	 * Call this method to stop the workflow defined by the id.
	 * @param id
	 * @throws Exception
	 */
	public void stop(final long id) throws Exception;
	
	/**
	 * Call this method to determine if a given workflow is active.
	 * @param id
	 * @throws Exception
	 */
	public boolean isActive(final long id) throws Exception;

	
	/**
	 * Returns the current output from the workflow.
	 */
	public String getStandardOut();
	
	/**
	 * Returns the current error from the workflow.
	 */
	public String getStandardError();

	/**
	 * Called to release memory used by the service.
	 * 
	 * This stops the JMX service and MUST be called, usually in a try finally block
	 */
	public void clear()  throws Exception;

	/**
	 * Call to tell the service to log everything it sees via a logger object,
	 * otherwise it saves everything it sees to a list of strings.
	 * 
	 * Default is true
	 * 
	 * @param b
	 */
	public void setLogToStandardOut(boolean b);
	
	/**
	 * Sets a system property into the running workflow JVM.
	 * This property can be tested in actors or anywhere in the workflow process.
	 * 
	 * Use with caution as the property might also be an important Java one.
	 * 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value);

}
