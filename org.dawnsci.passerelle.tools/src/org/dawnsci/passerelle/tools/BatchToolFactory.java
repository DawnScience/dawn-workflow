/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.passerelle.tools;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * A factory which can find batch tools registered by
 * plot tool id.
 * 
 * @author Matthew Gerring
 *
 */
public class BatchToolFactory {

	/**
	 * 
	 * @param plotToolId
	 * @return null if no batch tool registered for the plot tool, otherwise an instance of the batch tool.
	 */
	public static final IBatchTool getBatchTool(final String plotToolId) throws Exception {
		
		if (plotToolId==null) return null;
		final IConfigurationElement[] es = Platform.getExtensionRegistry().getConfigurationElementsFor("org.dawnsci.passerelle.tools.batchTool");
	    for (IConfigurationElement e : es) {
			
	    	final String ptid = e.getAttribute("tool_id");
	    	if (ptid.equals(plotToolId)) {
	    		IBatchTool bt = (IBatchTool)e.createExecutableExtension("class");
	    		if (bt instanceof AbstractBatchTool) {
	    			((AbstractBatchTool)bt).setBatchToolId(e.getAttribute("id"));
	    		}
	    		return bt;
	    	}
		}
		return null;
	}

	/**
	 * Tests if a batch tool exists without calling the constructor to make a new one.
	 * @param plotToolId
	 * @return
	 */
	public static boolean isBatchTool(String plotToolId) {
		
		if (plotToolId==null) return false;
		final IConfigurationElement[] es = Platform.getExtensionRegistry().getConfigurationElementsFor("org.dawnsci.passerelle.tools.batchTool");
	    for (IConfigurationElement e : es) {
			
	    	final String ptid = e.getAttribute("tool_id");
	    	if (ptid.equals(plotToolId)) return true;
		}
		return false;
	}
}
