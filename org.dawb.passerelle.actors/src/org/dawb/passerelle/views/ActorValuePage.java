/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.views;

import java.util.List;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.passerelle.actors.Activator;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.message.XPathVariable;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isencia.passerelle.workbench.model.editor.ui.editpart.ActorEditPart;
import com.isencia.passerelle.workbench.model.editor.ui.editpart.RelationEditPart;

/**
 * We wire this page between the Passerelle multipage editor and the Value view which is generic
 * using an extension point.
 * 
 * @author fcp94556
 *
 */
public class ActorValuePage extends Page implements ISelectionListener, IPartListener{

	private static Logger logger = LoggerFactory.getLogger(ActorValuePage.class);
	
	protected CLabel       label;
	protected SourceViewer sourceViewer;
	protected TableViewer  tableViewer;
	protected TableViewerColumn intTypeColumn, inNameColumn, inValueColumn, outTypeColumn, outNameColumn, outValueColumn;

	protected StructuredSelection lastSelection;

	protected Composite container;

	private boolean isTableView;

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		createControl(parent, true);
	}
	protected void createControl(Composite parent, boolean activeListeners) {
		
		this.container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(container);
		container.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		this.label  = new CLabel(container, SWT.LEFT);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		this.sourceViewer = new SourceViewer(container, null, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY );
		sourceViewer.setEditable(false);
		sourceViewer.getTextWidget().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    
		this.tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		
		createColumns(tableViewer);
		tableViewer.setUseHashlookup(true);
		tableViewer.setColumnProperties(new String[]{"Input Name", "Example Value", "Output Name", "Example Value"});
		
		if (activeListeners) {
			getSite().getPage().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
			getSite().getPage().addPartListener(this);
		
			try {
				updateSelection(EclipseUtils.getActivePage().getSelection());
			} catch (Throwable ignored) {
				// There might not be a selection or page.
			}
		}

		setTableView(false);
	}

	private Image listIcon, scalarIcon, roiIcon;
	
	private void createColumns(final TableViewer viewer) {
		
		listIcon   = Activator.getImageDescriptor("icons/list.png").createImage();
		scalarIcon = Activator.getImageDescriptor("icons/scalar.png").createImage();
		roiIcon    = Activator.getImageDescriptor("icons/roi.png").createImage();
		
		this.intTypeColumn   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		intTypeColumn.getColumn().setText(" ");
		intTypeColumn.getColumn().setWidth(0);
		intTypeColumn.getColumn().setResizable(false);
		intTypeColumn.setLabelProvider(new ColumnLabelProvider() {
			public Image getImage(Object element) {
				final ActorValueObject var = (ActorValueObject)element;
				if (var==null)                 return null;
				if (var.getInDataType()==null) return null;
				switch(var.getInDataType()) {
				case LIST:
					return listIcon;
				case SCALAR:					
					return scalarIcon;
				case ROI:					
					return roiIcon;
			    default:
					return null;
				}
			}			
			public String getText(Object element) {
				return "";
			}
		});
		
		this.inNameColumn   = new TableViewerColumn(viewer, SWT.LEFT, 1);
		inNameColumn.getColumn().setText("Input Name");
		inNameColumn.getColumn().setWidth(200);
		inNameColumn.setLabelProvider(new ActorValueLabelProvider(0));
		
		this.inValueColumn   = new TableViewerColumn(viewer, SWT.LEFT, 2);
		inValueColumn.getColumn().setText("Example Value");
		inValueColumn.getColumn().setWidth(300);
		inValueColumn.setLabelProvider(new ActorValueLabelProvider(1));
		
		this.outTypeColumn   = new TableViewerColumn(viewer, SWT.LEFT, 3);
		outTypeColumn.getColumn().setText(" ");
		outTypeColumn.getColumn().setWidth(0);
		outTypeColumn.getColumn().setResizable(false);
		outTypeColumn.setLabelProvider(new ColumnLabelProvider() {
			public Image getImage(Object element) {
				final ActorValueObject var = (ActorValueObject)element;
				if (var==null)                 return null;
				if (var.getOutDataType()==null) return null;
				switch(var.getOutDataType()) {
				case LIST:
					return listIcon;
				case SCALAR:					
					return scalarIcon;
				case ROI:					
					return roiIcon;
			    default:
					return null;
				}
			}			
			public String getText(Object element) {
				return "";
			}
		});

		this.outNameColumn  = new TableViewerColumn(viewer, SWT.LEFT, 4);
		outNameColumn.getColumn().setText("Output Name");
		outNameColumn.getColumn().setWidth(200);
		outNameColumn.setLabelProvider(new ActorValueLabelProvider(2));
		
		this.outValueColumn   = new TableViewerColumn(viewer, SWT.LEFT, 5);
		outValueColumn.getColumn().setText("Example Value");
		outValueColumn.getColumn().setWidth(300);
		outValueColumn.setLabelProvider(new ActorValueLabelProvider(3));
	}

	@Override
	public Control getControl() {
		return container;
	}
	
	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}

	public void dispose() {
		super.dispose();
		if (scalarIcon!=null) scalarIcon.dispose();
		if (listIcon!=null)   listIcon.dispose();
		if (roiIcon!=null)    roiIcon.dispose();
		getSite().getPage().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		getSite().getPage().removePartListener(this);
		lastSelection=null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		try {
			updateSelection(selection);
		} catch (Exception e) {
			logger.error("Cannot update value", e);
		}
	}

	protected void updateSelection(ISelection selection) throws Exception {
		
		if (selection instanceof StructuredSelection) {
			this.lastSelection = (StructuredSelection)selection;
			final Object sel = lastSelection.getFirstElement();
			
			final boolean didUpdate = updateObjectSelection(sel);				
			if (!didUpdate) clear();

			
			sourceViewer.refresh();
			label.getParent().layout(new Control[]{label, sourceViewer.getTextWidget()});
			
			return;
		}
		
		clear();
	}

	protected void setTableView(boolean isVisible) {
		this.isTableView = isVisible;
		GridUtils.setVisible(label,                     !isVisible);
		GridUtils.setVisible(sourceViewer.getControl(), !isVisible);
		GridUtils.setVisible(tableViewer.getControl(),  isVisible);
		
		label.getParent().layout(new Control[]{label,sourceViewer.getControl(),tableViewer.getControl()});
	}
	
	public Viewer getActiveViewer() {
		return isTableView ? tableViewer : sourceViewer;
	}


	/**
	 * Set it back to blank
	 */
	private void clear() {
		label.setText("");
		sourceViewer.getTextWidget().setText("");
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
				
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public Object[] getElements(Object inputElement) {
				return	new ActorValueObject[]{};
			}
		});
		tableViewer.setInput(new Object());
		tableViewer.refresh();
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part == this) {
			try {
				updateSelection(lastSelection);
			} catch (Throwable ignored) {
				// There might not be a selection or page.
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		
	}
	
	public boolean updateObjectSelection(Object sel)  throws Exception{

        if (sel == null) return true;
		if (sel instanceof XPathVariable) {
			setTableView(false);
			final XPathVariable var = (XPathVariable)sel;
			label.setText("Example value of variable '"+var.getVariableName()+"' with xpath '"+var.getxPath()+"'");
			sourceViewer.getTextWidget().setText(var.getExampleValue().toString());
            return true;
			
		} else if (sel instanceof Variable) {
			setTableView(false);
			final Variable var = (Variable)sel;
			label.setText("Example value of '"+var.getVariableName()+"':");
			sourceViewer.getTextWidget().setText(var.getExampleValue().toString());
            return true;
			
		} else if (sel instanceof AbstractGraphicalEditPart) {
			setTableView(true);
			updateNamedObject((AbstractGraphicalEditPart)sel);
            return true;
		}
		return false;
	}
	
	private void updateNamedObject(final AbstractGraphicalEditPart sel)  throws Exception {
		
		final Job uiUpdate = new Job("Updating Actor Inputs and Outputs") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (sel instanceof ActorEditPart) { // Actor
					final Object actor = ((ActorEditPart)sel).getActor();
					if (actor instanceof IVariableProvider) {
						final IVariableProvider prov = (IVariableProvider)actor;
						updateInputsOutputs(prov.getInputVariables(), prov.getOutputVariables(), true);
						return Status.OK_STATUS;
					}
				} else if (sel instanceof RelationEditPart) { // Wire
					final Object source   = ((ActorEditPart)((RelationEditPart)sel).getSource()).getActor();
				
					List<IVariable> inputVariables = null;
					if (source instanceof IVariableProvider) {
						final IVariableProvider prov = (IVariableProvider)source;
						inputVariables = prov.getOutputVariables();
					}					
					updateInputsOutputs(inputVariables, null, false);
					return Status.OK_STATUS;
				}
				
				updateInputsOutputs(null, null,true);
				return Status.OK_STATUS;
			}
			
		};
		uiUpdate.setSystem(true);
		uiUpdate.setUser(false); // Important do NOT show user progress here - a dialog continually pops up.
		                         // In fact why even use a job here? Little work is done in it and then it goes back
		                         // to the display thread.
		uiUpdate.setPriority(Job.SHORT);
		uiUpdate.schedule();
		
	}

	/**
	 * Must be called on the UI thread.
	 * @param bean
	 */
	public void setData(final UserDebugBean bean) {
		
		if (Display.getCurrent()==null) throw new RuntimeException("ActorValuePage.setData(...) must be called on the UI thread!");
		
		// Setup columns required.
		boolean isInputs = bean.getInputs()!=null && !bean.getInputs().isEmpty();
		if (isInputs) {
			intTypeColumn.getColumn().setWidth(30);
			intTypeColumn.getColumn().setResizable(true);
	
			inNameColumn.getColumn().setText("Input Name");
			inNameColumn.getColumn().setWidth(200);
			inNameColumn.getColumn().setResizable(true);
	
			inValueColumn.getColumn().setWidth(400);
			inValueColumn.getColumn().setText("Input Value");
			inValueColumn.getColumn().setResizable(true);
		} else {
			setColumnVisible(intTypeColumn, false);
			setColumnVisible(inNameColumn, false);
			setColumnVisible(inValueColumn, false);
		}
		
		// Setup columns required.
		boolean isOutputs = bean.getOutputs()!=null && !bean.getOutputs().isEmpty();
		if (isOutputs) {
			outTypeColumn.getColumn().setWidth(30);
			outTypeColumn.getColumn().setResizable(true);
	
			outNameColumn.getColumn().setWidth(200);
			outNameColumn.getColumn().setText("Output Name");
			outNameColumn.getColumn().setResizable(true);
			
			outValueColumn.getColumn().setWidth(400);
			outValueColumn.getColumn().setResizable(true);
			outValueColumn.getColumn().setText("Output Value");
		} else{
			setColumnVisible(outTypeColumn, false);
			setColumnVisible(outNameColumn, false);
			setColumnVisible(outValueColumn, false);
		
		}
		
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
				
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public Object[] getElements(Object inputElement) {
				final List<ActorValueObject> ret = ActorValueUtils.getTableObjects(bean);
				return	ret.toArray(new ActorValueObject[ret.size()]);
			}
		});
		tableViewer.setInput(new Object());
	}

	private void setColumnVisible(TableViewerColumn col, boolean vis) {
		if (vis) {
			col.getColumn().setWidth(100);
			col.getColumn().setResizable(true);
		} else{
			col.getColumn().setWidth(0);
			col.getColumn().setResizable(false);
		}
	}
	/**
	 * UI Thread safe method for updating the model
	 * @param inputVariables
	 * @param outputVariables
	 */
	private void updateInputsOutputs(final List<IVariable> in,
			                         final List<IVariable> out,
			                         final boolean isActor) {
		
		getSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				
				if (isActor) {
					inNameColumn.getColumn().setText("Input Name");
					inNameColumn.getColumn().setWidth(200);
					inValueColumn.getColumn().setWidth(300);
					outNameColumn.getColumn().setWidth(200);
					outNameColumn.getColumn().setResizable(true);
					outValueColumn.getColumn().setWidth(300);
					outValueColumn.getColumn().setResizable(true);
				} else {
					inNameColumn.getColumn().setText("Wire Variable Name");
					inNameColumn.getColumn().setWidth(200);
					inValueColumn.getColumn().setWidth(300);
					outNameColumn.getColumn().setWidth(0);
					outNameColumn.getColumn().setResizable(false);
					outValueColumn.getColumn().setWidth(0);
					outValueColumn.getColumn().setResizable(false);
				}
				
				tableViewer.setContentProvider(new IStructuredContentProvider() {
					@Override
					public void dispose() {
						
					}
					@Override
					public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

					@Override
					public Object[] getElements(Object inputElement) {
						final List<ActorValueObject> ret = ActorValueUtils.getTableObjects(in,out);
						return	ret.toArray(new ActorValueObject[ret.size()]);
					}
				});
				tableViewer.setInput(new Object());
				tableViewer.refresh();
			}		
		});
	}
}
