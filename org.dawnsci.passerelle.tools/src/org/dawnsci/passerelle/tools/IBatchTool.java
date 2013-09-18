package org.dawnsci.passerelle.tools;

import org.dawb.workbench.jmx.UserPlotBean;

import ptolemy.kernel.util.NamedObj;

/**
 * 
 * A tool, which is like a plotting tool, and can run in batch from workflows.
 * 
 * @author fcp94556
 *
 */
public interface IBatchTool {

	/**
	 * Call this method to process the tool the same as it would have been
	 * when run in batch. The bean  contains a method getToolData() which 
	 * the tool may cast back to data is requires to do the processing.
	 * 
	 * @param bean
	 * @return
	 */
	UserPlotBean process(UserPlotBean bean, NamedObj parent) throws Exception;

}
