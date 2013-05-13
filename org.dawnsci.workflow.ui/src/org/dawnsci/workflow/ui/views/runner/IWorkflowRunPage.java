package org.dawnsci.workflow.ui.views.runner;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISourceProvider;

/**
 * Please ensure that your implementation has a no argument constructor.
 * @author fcp94556
 *
 */
public interface IWorkflowRunPage {
		
    /**
     * 	
     * @return the title of your custom page. The user will not know what
     * a worklflow is so this should use the language of the custom technique they
     * would like to run.
     */
	public String getTitle();

	/**
	 * Creates the custom UI which will configure workflow configuration.
	 * 
	 * @param parent
	 * @return the composite you added which will be used for focus. Not the parent.
	 */
	public Composite createPartControl(Composite parent);

	
	/**
	 * Run with the current values.  The context provides a method for running
	 * the workflow so there is no need to copy workflow running around.
	 */
	public void run(IWorkflowContext context) throws Exception;

	
	/**
	 * Optionally implement getSourceProviders() if data binding has been
	 * used in your custom UI. This will be passed back into the run
	 * method to retrieve values. If null, null will be sent to the run
	 * method.
	 * 
	 * @return all the source providers in the custom UI created
	 * in create createPartControl, may be null.
	 * 
	 */
	public ISourceProvider[] getSourceProviders();

	
	/**
	 * Access to the underlying view; so that toolbars and menubars
	 * can be configured for instance.
	 * 
	 * This method will be called after the zro argument constuctor at the
	 * start of page creation.
	 * 
	 * @param view
	 */
	public void setWorkflowView(WorkflowRunView view);

	/**
	 * Called when containing view is disposed.
	 */
	public void dispose();

}
