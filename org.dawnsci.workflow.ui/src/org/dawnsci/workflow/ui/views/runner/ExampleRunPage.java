package org.dawnsci.workflow.ui.views.runner;

import org.dawnsci.common.widgets.file.ResourceChoiceBox;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISourceProvider;


/**
 * To use this page, see the commented out
 * section in plugin.xml of this plugin.
 * 
 * This shows how to set up the extension points correctly.
 * 
 * This class shows how to use the IWorkflowRunPage interface.
 * 
 * @author fcp94556
 *
 */
public class ExampleRunPage extends AbstractWorkflowRunPage {

	private ResourceChoiceBox fileBox;
	private Composite composite;

	@Override
	public String getTitle() {
		return "Maths example";
	}

	@Override
	public Composite createPartControl(Composite parent) {
		
        this.composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        
        this.fileBox   = new ResourceChoiceBox();
        fileBox.createContents(composite);
        
        // Try to find path of maths example and run it.
        final IProject workflows = ResourcesPlugin.getWorkspace().getRoot().getProject("workflows");
        if (workflows!=null) {
        	IResource mathsExample = workflows.getFile(new Path("examples/maths_example.moml"));
            fileBox.setResource(mathsExample);
        }
        
		return composite;
	}

	@Override
	public void run(final IWorkflowContext context) throws Exception {
		
		final String momlPath = fileBox.getAbsoluteFilePath();
		final Job run = new Job("Execute "+getTitle()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Execute "+momlPath, 2);
					context.execute(momlPath, true, monitor);
				} catch (Exception e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		run.schedule();
	}

	@Override
	public ISourceProvider[] getSourceProviders() {
		return null;
	}

	@Override
	public void dispose() {
		if (composite!=null) composite.dispose();
	}

}
