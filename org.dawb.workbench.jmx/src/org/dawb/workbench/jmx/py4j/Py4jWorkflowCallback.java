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

import java.util.Map;

import org.dawb.workbench.jmx.UserInputBean;

public interface Py4jWorkflowCallback {
	
	public void setActorSelected(String actorName, boolean isSelected);

	public boolean showMessage(String title, String message, int type);

	public Map<String, String> createUserInput(String actorName, Map<String, String> userValues);

}

