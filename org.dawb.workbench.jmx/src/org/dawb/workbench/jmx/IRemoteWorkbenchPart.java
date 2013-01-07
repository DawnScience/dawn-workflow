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


public interface IRemoteWorkbenchPart {
	
	/**
	 * 
	 * @author fcp94556
	 *
	 */
	public interface Closeable {
        public boolean close();
	}
	
	/**
	 * Used to create the composite, will have already been called when the system makes the IRemoteWorkbenchPart.
	 * @param container
	 * @param closeable
	 * @return
	 */
	public void createRemotePart(final Object container, Closeable closeable);

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

	/**
	 * Called by the stop action to stop the part interacting with the user.
	 */
	public void stop();
	
	/**
	 * Called by the confirm action to confirm the parts interaction with the user.
	 */
	public void confirm();

	/**
	 * 
	 * @return the widget, for instance a ColumnViewer or an IPlottingSystem, which the user part provides.
	 */
	public Object getViewer();
	
	/**
	 * 
	 * @return normally false but true if the user was shown something only and was not able to interact.
	 */
	public boolean isMessageOnly();

	/**
	 * Call to set focus on the part.
	 */
	public void setRemoteFocus();

	/**
	 * Usual dispose() method.
	 */
	public void dispose();

	/**
	 * If this is a part which has internal menus as standard, and not just a composite
	 * you can initialize menus on it. The Object will likely be base to IActionBars.
	 * @param actionBarsWrapper
	 */
	public void initializeMenu(Object iActionBars);

}
