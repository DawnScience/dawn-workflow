package org.dawnsci.workflow.ui.views.runner;

import org.dawnsci.workflow.ui.Activator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This view can run arbitrary workflows with a custom UI. Use it as follows:
 * 
 * 0. Depend on this plugin.
 * 1. In your plugin declare a view with an id using this class.
 * 2. In your plugin define an extension point for IWorkflowRunConfiguration and
 *    give it the id of your view. 
 *    
 * Now your view will have the custom UI and be able to run workflows using the 
 * IWorkflowContext passed into the run method when the run action is pressed.
 * 
 * @author fcp94556
 *
 */
public class WorkflowRunView extends ViewPart {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkflowRunView.class);
	
	private IWorkflowRunPage runner;
	private Composite        component;
	private Action           runAction;
	private Action           stopAction;
	
	/**
	 *  Gets the IWorkflowRunConfiguration from the extension point.
	 */
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        createWorkflowRunPage(site);
    }

	private void createWorkflowRunPage(IViewSite site) {
		try {
	        final IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.dawnsci.workflow.ui.workflowRunPage");
	        for (IConfigurationElement e : elements) {
				
	        	final String id = e.getAttribute("viewId");
	        	if (id.equals(site.getId())) {
	        		runner = (IWorkflowRunPage)e.createExecutableExtension("class");
	        		break;
	        	}
			}
		} catch (Throwable ne) {
			logger.error("Cannot assign the IWorkflowRunPage for '"+site.getId()+"'. Invalid view part created! Configuration error - please fix this.");
		    return;
		}
		if (runner!=null) runner.setWorkflowView(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		createActions();
		component  = runner.createPartControl(parent);
		
		// TODO Do we need a component at the bottom
		// with start and stop buttons?
		//createButtons();
	}

	private IWorkflowContext context;
	
	/**
	 * TODO Something to grey out actions when one is running.
	 */
	private void createActions() {
		
		this.stopAction = new Action("Stop "+runner.getTitle(), Activator.getImageDescriptor("icons/stop_workflow.gif")) {
			public void run() {
				try {
					if (context!=null) context.stop();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		};
		stopAction.setEnabled(false);
		stopAction.setId("org.dawnsci.workflow.ui.views.runner.stopAction");
		
		this.runAction = new Action("Run "+runner.getTitle(), Activator.getImageDescriptor("icons/run_workflow.gif")) {
			public void run() {
				
				try {
					stopAction.setEnabled(true);
					if (context!=null) context.stop();
					context = new WorkflowContext(WorkflowRunView.this, runner.getSourceProviders());
					runner.run(context);
					
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				} finally {
					if (context!=null && context.isRunning()) {
						stopAction.setEnabled(false);				
					}
				}
			}
		};
		runAction.setId("org.dawnsci.workflow.ui.views.runner.runAction");

		getViewSite().getActionBars().getToolBarManager().add(runAction);
		getViewSite().getActionBars().getToolBarManager().add(stopAction);
	}

	@Override
	public void setFocus() {
		if (component!=null) component.setFocus();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (runner!=null) runner.dispose();
	}

	public IAction getRunAction() {
		return runAction;
	}

	public IAction getStopAction() {
		return stopAction;
	}

}
