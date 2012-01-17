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

import java.util.Map;
import java.util.Queue;


public interface RemoveWorkbenchPart {

	/**
	 * 
	 * @param partName
	 */
	public void setPartName(String partName);

	
	/**
	 * This queue will be notified when the user confirms the values.
	 * @param valueQueue
	 */
	public void setQueue(final Queue<Map<String,String>> valueQueue);
	
	/**
	 * The default values that the user will edit.
	 * 
	 * NOTE Calling this will also refresh the UI table to reflect these new values.
	 * 
	 * @param values
	 */
	public void setValues(final Map<String,String> values);

	/**
	 * Information specifying which fields should be edited and how.
	 * @param configuration
	 */
	public void setConfiguration(String configurationXML) throws Exception;

}
