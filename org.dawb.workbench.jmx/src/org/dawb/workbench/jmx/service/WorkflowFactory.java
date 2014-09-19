/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.service;

import org.dawb.workbench.jmx.IRemoteServiceProvider;

/**
 * This factory returns a implementation of IWorkflowService
 * 
 * @author gerring
 *
 */
public class WorkflowFactory {
	
	/**
	 * Call this method obtain a reference to the IWorkflowService.
	 * This factory pattern is used to hide the implementation of the 
	 * service from external API
	 * 
	 * @return
	 */
	public static IWorkflowService createWorkflowService(final IRemoteServiceProvider prov) {
		return new WorkflowServiceImpl(prov);
	}
}
