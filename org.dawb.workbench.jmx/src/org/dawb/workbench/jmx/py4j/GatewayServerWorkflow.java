/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.py4j;


import org.dawb.workbench.jmx.service.IWorkflowService;
import org.dawb.workbench.jmx.service.WorkflowFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py4j.GatewayServer;

public class GatewayServerWorkflow {

	private Py4jWorkflowCallback py4jWorkflowCallback;
	
	private static Logger logger;

	private IWorkflowService service;

	private Process workflow;

	private String workspacePath;


	private String modelPath;

	private String installationPath;

	private boolean tangoSpecMode;

	private boolean serviceTerminate;

	/**
	 * Configure log4j
	 */
	private static final void createLoggingProperties() {
				
		logger = LoggerFactory.getLogger(GatewayServerWorkflow.class);
	}

	
	/**
	 * Set the Python callback reference
	 */
	public void setPy4jWorkflowCallback(Py4jWorkflowCallback thePy4jWorkflowCallback) {
		System.out.println("Setting the service provider!");
		py4jWorkflowCallback = thePy4jWorkflowCallback;
	}
	
	/**
	 * Run's the workflow!
	 */
	public void runWorkflow() throws Exception {
		
		//logger.debug("Starting the workflow!");
		service  = WorkflowFactory.createWorkflowService(new Py4jServiceProvider(this, py4jWorkflowCallback));
		workflow = service.start();
	}
	
	/**
	 * Waits till the workflow has finished
	 */
	public void synchronizeWorkflow() {
		try {
			// Waits until it is finished.
			workflow.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Clears the service running the workflow
	 */
	public void seviceClear() {
		// Release any memory used by the object
		try {
			service.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    
    
    public static void main(String[] args) throws Exception {
    	// This is an example of how to configure log4j. 
    	createLoggingProperties();        
    	GatewayServer server = new GatewayServer(new GatewayServerWorkflow());
        server.start();
        logger.info("Gateway server started.");
    }


	public void setWorkspacePath(String workspacePath) {
		this.workspacePath = workspacePath;
	}


	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}


	public void setInstallationPath(String installationPath) {
		this.installationPath = installationPath;
	}


	public void setTangoSpecMode(boolean tangoSpecMode) {
		this.tangoSpecMode = tangoSpecMode;
	}

	public void setServiceTerminate(boolean serviceTerminate) {
		this.serviceTerminate = serviceTerminate;
	}

	public String getWorkspacePath() {
		return workspacePath;
	}


	public String getModelPath() {
		return modelPath;
	}


	public String getInstallationPath() {
		return installationPath;
	}


	public boolean getTangoSpecMockMode() {
		return tangoSpecMode;
	}


	public boolean getServiceTerminate() {
		return serviceTerminate;
	}

	
	

}
