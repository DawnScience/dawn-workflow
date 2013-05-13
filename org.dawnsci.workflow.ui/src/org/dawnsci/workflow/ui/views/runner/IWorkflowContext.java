package org.dawnsci.workflow.ui.views.runner;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.ISourceProvider;

/**
 * This class provides resources for running a workflow in a custom way.
 * @author fcp94556
 *
 */
public interface IWorkflowContext {

	/**
	 * 
	 * @return a list of the data bound UI used in the custom workflow
	 *         or null if none were defined.
	 */
	public ISourceProvider[] getSourceProviders();
	
    /**
     * Runs the workflow at the given path. If sameVm is
     * true the current running VM is used (faster but more dangerous!)
     * 
     * No job is used, call the run method from a job unless you want the UI to
     * block.
     * 
     * This method is thread safe, you can call it from a Job.
     * 
     * @param momlPath
     * @param sameVm
     * @param monitor, may be null
     * @throws Exception if anything goes wrong with the run, or it is stopped.
     */
	public void execute(final String momlPath, boolean sameVm, IProgressMonitor monitor) throws Exception;

	/**
	 * Call this method to attempt to stop any running workflow
	 */
	public void stop() throws Exception;

	/**
	 * 
	 * @return true if something is being run.
	 */
	public boolean isRunning();
}
