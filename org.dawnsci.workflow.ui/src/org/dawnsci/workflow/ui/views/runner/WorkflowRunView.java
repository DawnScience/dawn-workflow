package org.dawnsci.workflow.ui.views.runner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
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
//	private Action           runAction;
//	private Action           stopAction;
	private Map<String, Action> runActions = new HashMap<String, Action>();
	private Map<String, Action> stopActions = new HashMap<String, Action>();
	private Map<String, String> workflowFiles = new HashMap<String, String>();
	private Map<String, String> runIcons = new HashMap<String, String>();
	private Map<String, String> stopIcons = new HashMap<String, String>();
	private int workflowFilesTotal = 2;
	private String workflowFileAttrib = "workflowFile";
	private String runIconAttrib = "runIcon";
	private String stopIconAttrib = "stopIcon";
	
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
	        		// Retrieve moml files and run icons
		        	for(int i = 0; i<workflowFilesTotal; i++){
		        		final String workflowFile = e.getAttribute(workflowFileAttrib+(i+1));
			        	final String runIcon = e.getAttribute(runIconAttrib+(i+1));
			        	final String stopIcon = e.getAttribute(stopIconAttrib+(i+1));
			        	if(workflowFile != null)
			        		workflowFiles.put(workflowFile, workflowFile);
			        	if(runIcon != null)
			        		runIcons.put(workflowFile, runIcon);
			        	if(stopIcon != null)
			        		stopIcons.put(workflowFile, stopIcon);
		        	}
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
	 * TODO replace "reference:file:" regex by something more sensible
	 */
	private void createActions() {
		Set<String> runIconList = runIcons.keySet();
//		Set<String> stopIconList = stopIcons.keySet();
		int i = 1;
		for (Iterator<String> iterator = runIconList.iterator(); iterator.hasNext();) {
			final String workflowFile = (String) iterator.next();
			Bundle bundle = FrameworkUtil.getBundle(runner.getClass());
			final String bundlePath = bundle.getLocation();
			
			ImageDescriptor runImageDescript = new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					String path = bundlePath.replaceFirst("reference:file:", "");
					ImageData id = new ImageData(path+runIcons.get(workflowFile));
					return id;
				}
			};
			Action runAction = new Action("Run "+runner.getTitles().get(workflowFile), runImageDescript) {
				@Override
				public void run() {
					try {
						stopActions.get(workflowFile).setEnabled(true);
						if (context!=null) context.stop();
						context = new WorkflowContext(WorkflowRunView.this, runner.getSourceProviders());
						context.setWorkflowFilePath(workflowFiles.get(workflowFile));
						runner.run(context);
						
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					} finally {
						if (context!=null && context.isRunning()) {
							stopActions.get(workflowFile).setEnabled(false);
						}
					}
				}
			};
			runAction.setId("org.dawnsci.workflow.ui.views.runner.runAction"+(i));

			getViewSite().getActionBars().getToolBarManager().add(runAction);
			runActions.put(workflowFile, runAction);

			ImageDescriptor stopImageDescript = new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					String path = bundlePath.replaceFirst("reference:file:", "");
					ImageData id = new ImageData(path+stopIcons.get(workflowFile));
					return id;
				}
			};
			Action stopAction = new Action("Stop "+runner.getTitles().get(workflowFile), stopImageDescript) {
				public void run() {
					try {
						if (context!=null) context.stop();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			};
			stopAction.setId("org.dawnsci.workflow.ui.views.runner.stopAction"+(i));

			getViewSite().getActionBars().getToolBarManager().add(stopAction);
			getViewSite().getActionBars().getToolBarManager().add(new Separator());
			stopActions.put(workflowFile, stopAction);
			i++;
		}
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

	public Map<String, Action> getRunActions(){
		return runActions;
	}

	public Map<String, Action> getStopActions(){
		return stopActions;
	}

	public IWorkflowContext getContext(){
		return context;
	}

}
