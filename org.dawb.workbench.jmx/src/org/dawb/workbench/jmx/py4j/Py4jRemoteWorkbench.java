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
import org.dawb.workbench.jmx.UserPlotBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Py4jRemoteWorkbench implements IRemoteWorkbench {

	Logger logger = LoggerFactory.getLogger(Py4jRemoteWorkbench.class);

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
			py4jWorkflowCallback.showMessage(title, message, type);
		} catch(Exception e) {
			logger.info(e.toString());
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
		Map<String,String> scalarValues = bean.getScalarValues();
		Map<String,String> newScalarValues;
		try {
			newScalarValues = py4jWorkflowCallback.createUserInput(bean.getActorName(), scalarValues);
		} catch(Exception e) {
			logger.info(e.toString());
			e.printStackTrace();
			newScalarValues = scalarValues;
		}
		return newScalarValues;
	}

	@Override
	public boolean setActorSelected(final String resourcePath, 
			                        final String actorName,
			                        final boolean isSelected, 
			                        final int colorCode) throws Exception {
		
		logger.info("Select Actor Requested");
		logger.info("Actor "+actorName+"; isSelected "+isSelected);
		try {
			py4jWorkflowCallback.setActorSelected(actorName, isSelected);
		} catch(Exception e) {
			logger.info(e.toString());
		}
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

	@Override
	public UserPlotBean createPlotInput(UserPlotBean bean) throws Exception {
		throw new Exception("Cannot interact with plot in py4j mode!");
	}

}

