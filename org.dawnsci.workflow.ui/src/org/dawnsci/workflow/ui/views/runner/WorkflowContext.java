package org.dawnsci.workflow.ui.views.runner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.ISourceProvider;

import com.isencia.passerelle.workbench.model.editor.ui.editor.actions.ExecuteActionEvent;
import com.isencia.passerelle.workbench.model.editor.ui.editor.actions.ExecuteActionListener;
import com.isencia.passerelle.workbench.model.editor.ui.editor.actions.RunAction;
import com.isencia.passerelle.workbench.model.editor.ui.editor.actions.StopAction;
import com.isencia.passerelle.workbench.model.launch.ModelRunner;

class WorkflowContext implements IWorkflowContext {
	
	private ISourceProvider[] providers;
	private WorkflowRunView   view;

	WorkflowContext(WorkflowRunView view, ISourceProvider[] providers) {
		this.view      = view;
		this.providers = providers;
	}

	@Override
	public ISourceProvider[] getSourceProviders() {
		return providers;
	}
	
	private ModelRunner modelRunner;

	@Override
	public void execute(String momlPath, boolean sameVm, IProgressMonitor monitor) throws Exception {
		
		if (sameVm) {
			modelRunner = new ModelRunner();
			modelRunner.runModel(momlPath,false);
			modelRunner = null;
			// RemoteWorkbenchImpl sets the status to the actor running.
			view.getViewSite().getActionBars().getStatusLineManager().setMessage("");
			
		} else {
			RunAction runAction = new RunAction();		
			// Try to find IFile or throw exception.
			IFile file = getResource(momlPath);
			if (file==null) throw new Exception("The path '"+momlPath+"' is not a file in a project in the workspace. This is required for running in own VM (as JDT is used).");
			runAction.addExecuteActionListener(new ExecuteActionListener() {	
				@Override
				public void stopRequested(ExecuteActionEvent evt) {
					view.getRunAction().setEnabled(true);
					view.getStopAction().setEnabled(false);
				}
				
				@Override
				public void executionRequested(ExecuteActionEvent evt) {
					view.getRunAction().setEnabled(false);
					view.getStopAction().setEnabled(true);
				}
				
				@Override
				public void buttonRefreshRequested(ExecuteActionEvent evt) {
					view.getViewSite().getActionBars().getToolBarManager().update(false);
				}
			});
			runAction.run(file, false);
		}

	}

	private IFile getResource(String fullPath) {
		
		final String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(fullPath));				
		if (res==null) {
			String localPath;
			try {
				localPath = fullPath.substring(workspacePath.length());
			} catch (StringIndexOutOfBoundsException ne) {
				localPath = fullPath;
			}
            res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(localPath));
		}
		if (res==null) {
            res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(workspacePath+fullPath));
		}
		
		if (res !=null && res instanceof IFile) return (IFile)res;
		return null;
	}

	@Override
	public void stop() {
		if (modelRunner!=null) {
			modelRunner.stop();
			modelRunner = null;
		} else {
			(new StopAction()).run();
		}
		view.getViewSite().getActionBars().getStatusLineManager().setMessage("");
	}

	@Override
	public boolean isRunning() {
		if (modelRunner!=null) return true;
		return (new StopAction()).isEnabled();
	}
 
}
