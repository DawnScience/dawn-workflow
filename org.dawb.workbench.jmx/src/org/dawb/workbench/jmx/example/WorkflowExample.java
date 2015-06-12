/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.example;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.dawb.workbench.jmx.ActorSelectedBean;
import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawb.workbench.jmx.UserDebugBean;
import org.dawb.workbench.jmx.UserInputBean;
import org.dawb.workbench.jmx.UserPlotBean;
import org.dawb.workbench.jmx.service.IWorkflowService;
import org.dawb.workbench.jmx.service.WorkflowFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is just an example to show how to run the workflow
 * remotely. There is an plugin called org.dawb.tango.server.workflow which 
 * contains a tango server with a real implementation.
 * 
 * @author gerring
 *
 */
public class WorkflowExample {

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

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		runWorkflow();
		
		System.exit(1);
	}

	protected static void runWorkflow() throws Exception {
		
		// This is an example of how to configure log4j. 
		createLoggingProperties();
		
        // Create a new service each time 
		final IWorkflowService service  = WorkflowFactory.createWorkflowService(new ExampleServiceProvider());
		final Process          workflow = service.start();
		
		workflow.waitFor(); // Waits until it is finished.
		
		// Release any memory used by the object
		service.clear();
		
	}

	/**
	 * Method to show how to run above in thread.
	 */
	private void exampleRunningInThread() {
		
		final Thread workflowThread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
			        // Create a new service each time 
					final IWorkflowService service  = WorkflowFactory.createWorkflowService(new ExampleServiceProvider());
					final Process          workflow = service.start();
					
					try {
						// 1. Set tango state to running.
						//...
						
						// 2. Wait until workflow is finished.
						workflow.waitFor();

					} finally {
						// 3. Release any memory used by the object and close agent JMX service
						service.clear();
						
						// 4. Set tango state to not running.
						//...
					}
						
					
				} catch (Exception ne) {
					logger.error("Cannot run workflow using Tigermoth", ne);
				}

			}
		});
		
		// Always name your threads, this makes debugging easier
		workflowThread.setName("Tigermoth Workflow Thead");
		
		// Use start method to start a new thread.
		workflowThread.start();
		
	}
	
	public static class ExampleServiceProvider implements IRemoteServiceProvider {

		@Override
		public IRemoteWorkbench getRemoteWorkbench() throws Exception {
			return new ExampleRemoteWorkbench();
		}

		@Override
		public int getStartPort() {
			return 21701;
		}

		@Override
		public String getWorkspacePath() {
			return "/users/svensson/debug_workspaces";
		}

		@Override
		public String getModelPath() {
			return "/users/svensson/debug_workspace/workflows/examples/command_example.moml";
		}

		@Override
		public String getInstallationPath() {
			// Executable name of your eclipse worflow application, for instance dawb or sda.
			return "/opt/dawb/dawb";
		}

		@Override
		public boolean getTangoSpecMockMode() {
			// To test a TANGO Spec connection set to false
			return false;
		}

		@Override
		public boolean getServiceTerminate() {
			// Set to false to not have Eclipse to open a trace message in a new window
			return true;
		}

		@Override
		public Properties getProperties() {
			return null;
		}
	}
	
	
	public static class ExampleRemoteWorkbench implements IRemoteWorkbench {

		private Map<String, Object> mockValues;
		private boolean tangoSpecMockMode = true;

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
			final Map<String,String> ret = new HashMap<String,String>(3);
			ret.put("x", "1");
			return ret;
		}

		@Override
		public boolean setActorSelected(final ActorSelectedBean bean) throws Exception {
			
			logger.info("Select Actor Requested");
			logger.info("Actor "+bean.getActorName()+"; isSelected "+bean.isSelected());
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
			
			logger.info("Create Plot Input Requested");
			logger.info("Actor "+bean.getPartName());
			try {
				Thread.sleep(1000);// Simulate user pressing ok...
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 			
			return null;
		}
		
		@Override
		public UserDebugBean debug(final UserDebugBean bean) throws Exception {
			
			logger.info("Debug Requested");
			logger.info("Actor "+bean.getPartName());
			try {
				Thread.sleep(1000);// Simulate user pressing ok...
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 			
			return null;
		}

	}

}
