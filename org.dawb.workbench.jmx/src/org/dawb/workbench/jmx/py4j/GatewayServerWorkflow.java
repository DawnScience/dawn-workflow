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


import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawb.workbench.jmx.UserInputBean;
import org.dawb.workbench.jmx.example.WorkflowExample;
import org.dawb.workbench.jmx.example.WorkflowExample.ExampleRemoteWorkbench;
import org.dawb.workbench.jmx.service.IWorkflowService;
import org.dawb.workbench.jmx.service.WorkflowFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py4j.GatewayServer;

public class GatewayServerWorkflow {

	private IRemoteServiceProvider remoteServiceProvider = null;

	private Py4jWorkflowCallback py4jWorkflowCallback;
	
	private static Logger logger;

	/**
	 * Example of how to configure log4j if needed.
	 */
	private static final void createLoggingProperties() {
		
		if (System.getProperty("logback.configurationFile")==null) {
			final URL url = WorkflowExample.class.getResource("logback.xml");
			System.setProperty("logback.configurationFile", url.getFile());
		}
		
		logger = LoggerFactory.getLogger(WorkflowExample.class);
	}

	
	public void setPy4jWorkflowCallback(Py4jWorkflowCallback thePy4jWorkflowCallback) {
		System.out.println("Setting the service provider!");
		py4jWorkflowCallback = thePy4jWorkflowCallback;
		//System.out.println(remoteServiceProvider);
	}
	
	public void runWorkflow() throws Exception {
		
		// This is an example of how to configure log4j. 
		//createLoggingProperties();
		System.out.println("Starting the workflow!");
        // Create a new service each time 
		py4jWorkflowCallback.setActorSelected("Test!");
		final IWorkflowService service  = WorkflowFactory.createWorkflowService(new Py4jServiceProvider(py4jWorkflowCallback));
		final Process          workflow = service.start();
//		
//		workflow.waitFor(); // Waits until it is finished.
//		
//		// Release any memory used by the object
//		service.clear();
		
	}

    
    
    public static void main(String[] args) throws Exception {
    	// This is an example of how to configure log4j. 
    	createLoggingProperties();
        
    	GatewayServer server = new GatewayServer(new GatewayServerWorkflow());
        server.start();
        System.out.println("Gateway server started.");
    }

	
	

}
