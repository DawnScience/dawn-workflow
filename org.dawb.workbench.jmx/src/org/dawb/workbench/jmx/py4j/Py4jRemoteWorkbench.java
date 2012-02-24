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

import java.util.HashMap;
import java.util.Map;

import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawb.workbench.jmx.UserInputBean;
import org.dawb.workbench.jmx.example.WorkflowExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Py4jRemoteWorkbench implements IRemoteWorkbench {

	Logger logger = LoggerFactory.getLogger(WorkflowExample.class);

	private Map<String, Object> mockValues;
	private boolean tangoSpecMockMode = true;

	private Py4jWorkflowCallback py4jWorkflowCallback;

	public Py4jRemoteWorkbench(Py4jWorkflowCallback thePy4jWorkflowCallback) {
		py4jWorkflowCallback = thePy4jWorkflowCallback;
	}

	@Override
	public void executionStarted() {
		// Start notification
	}
	
	@Override
	public void executionTerminated(final int returnCode) {
		// No need to implement unless in RCP GUI.
	}
	
	@Override
	public boolean openFile(String fullPath) {
		logger.info("File Open Requested");
		logger.info("Path "+fullPath);
		return true;
	}

	@Override
	public boolean monitorDirectory(String fullPath, boolean startMonitoring) {
		logger.info("Directory Monitor Requested");
		logger.info("Path "+fullPath);
		return true;
	}

	@Override
	public boolean refresh(String projectName, String resourcePath) {
		logger.info("Refresh Requested");
		logger.info("Project "+projectName+"; path "+resourcePath);
		return true;
	}

	@Override
	public boolean showMessage(String title, String message, int type) {
		logger.info("Show Message Requested");
		logger.info("Title "+title+"; message "+message);
		try {
			Thread.sleep(1000);// User is pressing ok...
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		return true;
	}

	@Override
	public void logStatus(String pluginId, String message, Throwable throwable) {
		logger.error(message, throwable);
	}

	@Override
	public Map<String, String> createUserInput(final UserInputBean bean) throws Exception {
		logger.info("Create User Input Requested");
		logger.info("Actor "+bean.getPartName());
		try {
			Thread.sleep(1000);// User is pressing ok...
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 			
		final Map<String,String> ret = bean.getScalarValues();
		return ret;
	}

	@Override
	public boolean setActorSelected(final String resourcePath, 
			                        final String actorName,
			                        final boolean isSelected, 
			                        final int colorCode) throws Exception {
		
		logger.info("Select Actor Requested");
		logger.info("Actor "+actorName+"; isSelected "+isSelected);
		if ((actorName != null) && (py4jWorkflowCallback != null) && isSelected)
			py4jWorkflowCallback.setActorSelected(actorName);
		return true;
	}

	@Override
	public void setMockMotorValue(String motorName, Object value) {
		if (mockValues==null) mockValues = new HashMap<String,Object>(3);
		mockValues.put(motorName, value);
	}

	@Override
	public Object getMockMotorValue(String motorName) {
		if (mockValues==null) return null;
		return mockValues.get(motorName);
	}

	@Override
	public void notifyMockCommand(String motorName, String message, String cmd) {
		logger.info("Mock Notify Requested");
		logger.info("Motor "+motorName+"; message "+message);
	}

}

