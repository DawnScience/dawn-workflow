/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawb.workbench.jmx;


/**
 * Part used internally with UserModifyDialog and UserModifyEditor.
 * 
 * @author fcp94556
 *
 */
public interface IDeligateWorkbenchPart extends IRemoteWorkbenchPart {

	
	/**
	 * Used to create the composite, will have already been called when the system makes the IRemoteWorkbenchPart.
	 * @param container
	 * @param closeable
	 * @return
	 */
	public void createRemotePart(final Object container, Closeable closeable);

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
