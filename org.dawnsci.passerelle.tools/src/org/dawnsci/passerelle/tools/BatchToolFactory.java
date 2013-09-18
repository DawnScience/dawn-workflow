package org.dawnsci.passerelle.tools;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * A factory which can find batch tools registered by
 * plot tool id.
 * 
 * @author fcp94556
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
