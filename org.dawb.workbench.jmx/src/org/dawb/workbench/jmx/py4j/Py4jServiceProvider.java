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

import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.IRemoteWorkbench;

public class Py4jServiceProvider implements IRemoteServiceProvider {

	private GatewayServerWorkflow gatewayServerWorkflow;
	private Py4jWorkflowCallback py4jWorkflowCallback;

	public Py4jServiceProvider(GatewayServerWorkflow theGatewayServerWorkflow, 
			Py4jWorkflowCallback thePy4jWorkflowCallback) {
		gatewayServerWorkflow = theGatewayServerWorkflow;
		py4jWorkflowCallback = thePy4jWorkflowCallback;
	}

	@Override
	public IRemoteWorkbench getRemoteWorkbench() throws Exception {
		return new Py4jRemoteWorkbench(py4jWorkflowCallback);
	}

	@Override
	public int getStartPort() {
		return 21701;
	}

	@Override
	public String getWorkspacePath() {
		return gatewayServerWorkflow.getWorkspacePath();
	}

	@Override
	public String getModelPath() {
		return gatewayServerWorkflow.getModelPath();
	}

	@Override
	public String getInstallationPath() {
		// Executable name of your eclipse worflow application, for instance dawb or sda.
		return gatewayServerWorkflow.getInstallationPath();
	}

	@Override
	public boolean getTangoSpecMockMode() {
		// To test a TANGO Spec connection set to false
		return gatewayServerWorkflow.getTangoSpecMockMode();
	}

	@Override
	public boolean getServiceTerminate() {
		// Set to false to not have Eclipse to open a trace message in a new window
		return gatewayServerWorkflow.getServiceTerminate();
	}

}
