package org.dawb.passerelle.ui.views;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.common.actors.IDescriptionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ptolemy.actor.Actor;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

import com.isencia.passerelle.editor.common.model.PaletteBuilder;
import com.isencia.passerelle.workbench.model.editor.ui.editpart.ActorEditPart;

import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Text;

/**
 * Shows any help with attributes that was created 
 * with the attribute in the actor.
 * 
 * @author fcp94556
 *
 */
public class WorkflowDocumentationView extends ViewPart implements ISelectionListener{

	private final FormToolkit formToolkit;
	private Form form;
	private Label type;
	private PaletteBuilder builder;
	private Text usage;
	
	public WorkflowDocumentationView() {
		formToolkit = new FormToolkit(Display.getDefault());
		builder     = new PaletteBuilder();
	}

	@Override
	public void createPartControl(Composite parent) {
		
		this.form = formToolkit.createForm(parent);
		formToolkit.paintBordersFor(form);
		form.setText("...");
		
		Label lblType = formToolkit.createLabel(form.getBody(), "Type", SWT.NONE);
		lblType.setBounds(10, 10, 70, 20);
		
		type = formToolkit.createLabel(form.getBody(), " ", SWT.NONE);
		type.setAlignment(SWT.RIGHT);
		type.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		type.setBounds(60, 10, 228, 20);
		
		Label lblRequirement = formToolkit.createLabel(form.getBody(), "Requirement", SWT.NONE);
		lblRequirement.setBounds(10, 41, 85, 20);
		
		Label label = formToolkit.createLabel(form.getBody(), " ", SWT.NONE);
		label.setAlignment(SWT.RIGHT);
		label.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		label.setBounds(101, 41, 187, 20);
		
		Label lblUsage = formToolkit.createLabel(form.getBody(), "Usage", SWT.NONE);
		lblUsage.setBounds(10, 67, 70, 20);
		
		usage = formToolkit.createText(form.getBody(), " ", SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		usage.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		usage.setBounds(10, 93, 278, 254);

		EclipseUtils.getPage().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}
	
	public void dispose() {
		EclipseUtils.getPage().removeSelectionListener(this);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		
		final Object object = selection instanceof IStructuredSelection
				            ? ((IStructuredSelection)selection).getFirstElement()
				            : null;
				    
		Nameable nameable = null;
	    if (object instanceof ActorEditPart) {
	    	ActorEditPart edPart = (ActorEditPart)object;
	    	Actor actor = edPart.getActor();
	    	nameable    = actor;
	    	final String type = builder.getType(actor.getClass().getName());
	    	if (type!=null) this.type.setText(type);
	    	//this.type.setImage(image);
	    	
	    } else if (object instanceof Attribute) {
	    	Attribute param = (Attribute)object;
	    	nameable = (Nameable)param;
	    	String type = param.getClass().getSimpleName();
	    	if (type!=null && "".equals(type.trim())) {
	    		type = param.getClass().getSuperclass().getSimpleName();
	    	}
	    	if (type!=null) this.type.setText(type);
	    }
	    
	    if (nameable!=null) {
	    	String name = nameable.getDisplayName();
	    	if (name==null) name = nameable.getName();
	    	form.setText(name);
	    	
	    	if (nameable instanceof IDescriptionProvider) {
	    		IDescriptionProvider prov = (IDescriptionProvider)nameable;
	    		usage.setText(prov.getDescription());
	    	}
	    }
	}
}
