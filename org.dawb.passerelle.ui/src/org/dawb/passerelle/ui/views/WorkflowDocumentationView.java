package org.dawb.passerelle.ui.views;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.common.actors.IDescriptionProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

import com.isencia.passerelle.workbench.model.editor.ui.palette.PaletteBuilder;
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
	private Label lblRequirement;
	private Label requiredOrIcon;
	private Label variableHandling;
	private Label lblExpressionHandling;
	
	public WorkflowDocumentationView() {
		formToolkit = new FormToolkit(Display.getDefault());
		builder     = PaletteBuilder.getInstance();
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
		
		lblRequirement = formToolkit.createLabel(form.getBody(), "Requirement", SWT.NONE);
		lblRequirement.setBounds(10, 41, 85, 20);
		
		requiredOrIcon = formToolkit.createLabel(form.getBody(), " ", SWT.NONE);
		requiredOrIcon.setAlignment(SWT.RIGHT);
		requiredOrIcon.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		requiredOrIcon.setBounds(101, 41, 187, 20);
		
		Label lblUsage = formToolkit.createLabel(form.getBody(), "Usage", SWT.NONE);
		lblUsage.setBounds(10, 99, 70, 20);
		
		usage = formToolkit.createText(form.getBody(), " ", SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		usage.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		usage.setBounds(10, 119, 278, 254);
		
		this.lblExpressionHandling = formToolkit.createLabel(form.getBody(), "Expression Handling", SWT.NONE);
		lblExpressionHandling.setBounds(10, 67, 135, 20);
		
		variableHandling = formToolkit.createLabel(form.getBody(), "NONE", SWT.WRAP);
		variableHandling.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		variableHandling.setBounds(153, 67, 135, 47);

		updateSelection(EclipseUtils.getPage().getSelection());
		EclipseUtils.getPage().addSelectionListener(this);
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}
	
	public void dispose() {
		if (EclipseUtils.getPage()!=null) {
			EclipseUtils.getPage().removeSelectionListener(this);
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {				
	    updateSelection(selection);
	}

	private void updateSelection(ISelection selection) {

		clear();
		
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
	    	lblRequirement.setText("Icon");
	    	
	    	final Object icon = builder.getIcon(actor.getClass().getName());
	        if (icon instanceof ImageDescriptor) {
	        	ImageDescriptor des  = (ImageDescriptor)icon;
			    requiredOrIcon.setImage(des.createImage());
	        }
			
	    } else if (object instanceof Attribute) {
	    	Attribute param = (Attribute)object;
	    	nameable = (Nameable)param;
	    	StringBuilder type = new StringBuilder(param.getClass().getSimpleName().trim());
	    	if (type!=null && type.length()<1) {
	    		type.append(param.getClass().getSuperclass().getSimpleName());
	    	}
	    	if (param instanceof Parameter) {
	    		try {
					Parameter p = (Parameter)param;
					if (p.getToken()!=null) {
						type.append(" ("+p.getToken().getType()+")");
					}
				} catch (IllegalActionException e) {
					//e.printStackTrace();
				}
	    	}
	    	
	    	if (type.length()>0) this.type.setText(type.toString());
	    	
	    	lblRequirement.setText("Requirement");
	    	if (param.getContainer() instanceof IDescriptionProvider) {
	    		IDescriptionProvider prov = (IDescriptionProvider)param.getContainer();
	    	    requiredOrIcon.setText(prov.getRequirement(param).toString());	    	    
	    	    variableHandling.setText(prov.getVariableHandling(param).toString());
	    	    lblExpressionHandling.setText("Expression Handling");
	    	}
	    }
	    
	    if (nameable!=null) {
	    	String name = nameable.getDisplayName();
	    	if (name==null) name = nameable.getName();
	    	form.setText(name);
	    	
	    	if (nameable instanceof IDescriptionProvider) {
	    		IDescriptionProvider prov = (IDescriptionProvider)nameable;
	    		usage.setText(prov.getDescription()!=null ? prov.getDescription() : "");
	    	} else if (nameable.getContainer() instanceof IDescriptionProvider) {
	    		IDescriptionProvider prov = (IDescriptionProvider)nameable.getContainer();
	    		String desc = prov.getDescription(nameable);
	    		usage.setText(desc!=null ? desc : "");
	    	}
	    }
	}

	private void clear() {
		form.setText("");
		type.setText("");
		usage.setText("");
		lblRequirement.setText("");
		requiredOrIcon.setText("");
		requiredOrIcon.setImage(null);
		variableHandling.setText("");
		lblExpressionHandling.setText("");
	}
}
