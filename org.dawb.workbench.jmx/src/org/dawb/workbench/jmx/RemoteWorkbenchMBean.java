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

import javax.management.NotificationBroadcaster;


/**
 * The interface which is deployed 
 * for managing the workflow.
 */
public interface RemoteWorkbenchMBean extends IRemoteWorkbench, NotificationBroadcaster {
	
}
