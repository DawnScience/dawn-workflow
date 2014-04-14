/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.remote;

import javax.management.MBeanServerConnection;

import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.passerelle.workbench.model.editor.ui.editor.actions.ExecutionAction;
import com.isencia.passerelle.workbench.model.launch.IModelListener;

public class ModelListener implements IModelListener {

	private static final Logger logger = LoggerFactory.getLogger(ModelListener.class);

	@Override
	public void executionStarted() {
		try {
			MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			if (client!=null) client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "executionStarted", null, null);
			
		} catch (Exception ne) {
			logger.error("Cannot notify of terminate!", ne);
		}
	}

	@Override
	public void executionTerminated(int returnCode) {
		try {
			MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			if (client!=null) client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "executionTerminated", new Object[]{returnCode}, new String[]{int.class.getName()});
			
		} catch (Exception ne) {
			logger.error("Cannot notify of terminate!", ne);
		}
	}

	public static void notifyExecutionTerminated(int returnCode) {
		ExecutionAction.notifyExecutionTerminated(returnCode);
	}

	public static void notifyExecutionStarted() {
		ExecutionAction.notifyExecutionStarted();
	}

}
