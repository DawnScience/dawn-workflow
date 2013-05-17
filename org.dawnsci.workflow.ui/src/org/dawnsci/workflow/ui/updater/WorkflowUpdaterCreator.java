package org.dawnsci.workflow.ui.updater;

public class WorkflowUpdaterCreator {

	/**
	 * 
	 * @param dataFilePath
	 * @param modelFilePath
	 * @return
	 *       a workflow updater
	 */
	public static IWorkflowUpdater createWorkflowUpdater(String dataFilePath, String modelFilePath){
		return new WorkflowUpdaterImpl(dataFilePath, modelFilePath);
	}
}
