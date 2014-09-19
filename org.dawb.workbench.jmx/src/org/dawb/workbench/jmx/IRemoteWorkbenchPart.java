/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx;

import java.util.Map;
import java.util.Queue;

/**
 * IRemoteWorkbenchPart provides methods which can be done in the implementation
 * of IRemoteWorkbench to parts opened.
 * 
 * @author Matthew Gerring
 *
 */
public interface IRemoteWorkbenchPart {
	
	/**
	 * 
	 * @author Matthew Gerring
	 *
	 */
	public interface Closeable {
        public boolean close();
	}

	/**
	 * Carries data used by the remote workbench part to configure it.
	 * 
	 * May be cast or used by the RemoteWorkbenchPart in some way to configure it.
	 * 
	 * @param userObject
	 */
	public void setUserObject(Object userObject);

	/**
	 * 
	 * @param partName
	 */
	public void setPartName(String partName);

	
	/**
	 * This queue will be notified when the user confirms the values.
	 * An object will be added to this queue, one only, when the user
	 * is done editing. This will then be passed back.
	 * 
	 * @param valueQueue
	 */
	public void setQueue(final Queue<Object> valueQueue);
	
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
