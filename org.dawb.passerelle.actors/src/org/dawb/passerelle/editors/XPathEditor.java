/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.editors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.widgets.DoubleClickModifier;
import org.dawb.common.util.io.PropUtils;
import org.dawb.passerelle.actors.Activator;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.VariableUtils;
import org.dawb.passerelle.common.message.XPathVariable;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.CompositeActor;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.Workspace;

import com.isencia.passerelle.model.util.MoMLParser;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;

public class XPathEditor extends EditorPart {
	
    private static Logger logger = LoggerFactory.getLogger(XPathEditor.class);
	
	private TableViewer           viewer;
	private final String          momlPath;
	private final String          actorName;
	private XPathParticipant      actor;
	private List<IVariable>       upstream;
	private List<IVariable>       variables;
	private boolean               isDirty=false;
	private CLabel                messageLabel;	
	private ToolBarManager        toolMan;
	private MenuManager           menuMan;
	
	public XPathEditor(final String actorName, final String momlPath) {
		
		this.actorName  = actorName;
		this.momlPath   = momlPath;
	}

	@Override
	public void createPartControl(Composite parent) {
		
        final Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout());
        GridUtils.removeMargins(main);
        
		final Composite tools = new Composite(main, SWT.RIGHT);
		tools.setLayout(new GridLayout(2, false));
		GridUtils.removeMargins(tools);
		tools.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		this.messageLabel = new CLabel(tools, SWT.NONE);
		final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gridData.widthHint = 230;
		messageLabel.setLayoutData(gridData);
		messageLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		messageLabel.setToolTipText("Create variables from the output xml using XPaths.");
		final Image editableImage = Activator.getImageDescriptor("icons/editable.png").createImage();
		messageLabel.setImage(editableImage);
		messageLabel.setText("Edit/create output variables");

		this.toolMan = new ToolBarManager(SWT.FLAT|SWT.LEFT);
		final ToolBar          toolBar = toolMan.createControl(tools);
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		EdnaActorActions.createOutputActions(toolMan);
      
        this.viewer = new TableViewer(main, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(viewer,ToolTip.NO_RECREATE); 
        
        createColumns(viewer);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer.setUseHashlookup(true);
		viewer.setColumnProperties(new String[]{"Name","XPath","Rename Tag"});
        
		viewer.setCellEditors(createCellEditors(viewer));
		viewer.setCellModifier(createModifier(viewer));
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {		
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateActions();
			}
		});
				
		toolMan.update(true);
		
		getSite().setSelectionProvider(viewer);
		
		createRightClickMenu();
		updateActions();
 	}
	
	private void createRightClickMenu() {
	    this.menuMan = new MenuManager();
	    viewer.getControl().setMenu (menuMan.createContextMenu(viewer.getControl()));
		EdnaActorActions.createOutputActions(menuMan);
	}
	
	private void updateActions() {
		EdnaActorActions.deleteXPath.setEnabled(getSelected()!=null);
	}

	private CellEditor[] createCellEditors(final TableViewer tableViewer) {
		
		CellEditor[] editors  = new CellEditor[3];
		TextCellEditor textEd = new TextCellEditor(tableViewer.getTable());
		((Text)textEd.getControl()).setTextLimit(255);
		// NOTE Must not add verify listener - it breaks things.
		editors[0] = textEd;
		editors[1] = textEd;
		editors[2] = textEd;
		
		return editors;
	}
	
	private ICellModifier createModifier(final TableViewer tableViewer) {
		return new DoubleClickModifier(tableViewer) {
			@Override
			public boolean canModify(Object element, String property) {
				if (!enabled) return false;
				final IVariable var = (IVariable)element;
				if (!(var instanceof XPathVariable)) return false;
				try {
					if (upstream.contains(var)) return false;
				} catch (Exception e) {
					logger.error("Cannot read output variables for "+actor.getName(), e);
				}
				return true;// Both name, xpath and rename are editable.
			}

			@Override
			public Object getValue(Object element, String property) {
				final IVariable var = (IVariable)element;
				if (!(var instanceof XPathVariable)) return null;
				String value = null;
				if ("Name".equals(property)) {
					value = ((XPathVariable)element).getVariableName();
				} else if ("XPath".equals(property)) {
					value = ((XPathVariable)element).getxPath();
				} else if ("Rename Tag".equals(property)) {
					value = ((XPathVariable)element).getRename();
				}
				return value!=null ? value : "";
			}
			
			@Override
			public void modify(Object item, String property, Object value) {
				
				try {
				    final IVariable var = getSelected();
					if (!(var instanceof XPathVariable)) return;
					
					final XPathVariable xVar = (XPathVariable)var;
					if ("Name".equals(property)) {
						xVar.setVariableName((String)value);
					} else if ("XPath".equals(property)) {
						final String xPath = (String)value;
						xVar.setxPath(xPath);
						
					} else if ("Rename Tag".equals(property)) {
						final String rename = (String)value;
						xVar.setRename(rename);
					}
					
					// Recalculate the example value
					final String extract = actor.getExampleValue(xVar.getxPath(), xVar.getRename());
					xVar.setExampleValue(extract);

					
					isDirty = true;
					viewer.refresh();
					XPathEditor.this.firePropertyChange(PROP_DIRTY);
					viewer.setSelection(new StructuredSelection(xVar));
					
				} catch (Exception e) {
					logger.error("Cannot set "+property, e);
	
				} finally {
					setEnabled(false);
				}
			}
	    };
	}

	@Override
	public void setFocus() {
		
		viewer.getTable().setFocus();		
		createActorContentProvider();
	}

	private void createActorContentProvider() {
				
		MoMLParser.purgeAllModelRecords();
		final MoMLParser parser = new MoMLParser(new Workspace());
		try {
			final IResource   moml  = ResourcesPlugin.getWorkspace().getRoot().findMember(momlPath);
			moml.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			
			CompositeActor toplevel = (CompositeActor) parser.parse(null, new File(moml.getLocation().toString()).toURL());
			toplevel.workspace().setName(moml.getProject().getName());
			
			ComponentEntity entity  = ModelUtils.findEntityByName(toplevel, actorName);

			if (entity!=null && entity.getName().equals(actorName)) {
				
				this.actor = (XPathParticipant)entity;
				
				this.variables = createVariables(actor.getXPathVariables());
				this.upstream  = createVariables(actor.getUpstreamVariables());
				for (Iterator it = upstream.iterator(); it .hasNext();) {
					if (variables.contains(it.next())) it.remove();
				}
				
				viewer.setContentProvider(new IStructuredContentProvider() {
					@Override
					public void dispose() {
					}
					@Override
					public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

					@Override
					public Object[] getElements(Object inputElement) {
						if (actor==null) return new Object[]{""};
						final List<IVariable> ret = new ArrayList<IVariable>(7);
						if (variables!=null) ret.addAll(variables);
						if (upstream!=null)  ret.addAll(upstream);
						
						if (ret.size()>0) {
							return ret.toArray(new IVariable[ret.size()]);
						}
						return new Object[]{""};
					}
				});
				viewer.setInput(new Object());
			}

		} catch (Exception e) {
			logger.error("Cannot parse "+momlPath, e);
		}

	}


	private List<IVariable> createVariables(List<IVariable> xPathVariables) {
		
		final Collection<IVariable> tmp = new TreeSet<IVariable>(new XPathComparitor(actor));
		tmp.addAll(xPathVariables);
		
        final List<IVariable> ret = new ArrayList<IVariable>(7);
        ret.addAll(tmp);

        return ret;
	}

	private void createColumns(final TableViewer viewer) {
		
        TableViewerColumn var   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		var.getColumn().setText("Name");
		var.getColumn().setWidth(300);
		var.setLabelProvider(new XpathLabelProvider(this, 0, viewer.getTable().getFont()));

		var   = new TableViewerColumn(viewer, SWT.LEFT, 1);
		var.getColumn().setText("XPath");
		var.getColumn().setWidth(600);
		var.setLabelProvider(new XpathLabelProvider(this, 1, viewer.getTable().getFont()));
		
		var   = new TableViewerColumn(viewer, SWT.LEFT, 2);
		var.getColumn().setText("Rename Tag");
		var.getColumn().setWidth(300);
		var.setLabelProvider(new XpathLabelProvider(this, 2, viewer.getTable().getFont()));

	}
	
    public void dispose() {
    	super.dispose();
    	viewer.getTable().dispose();
    }

	public XPathParticipant getActor() {
		return actor;
	}


	@Override
	public void doSave(IProgressMonitor monitor) {
		
		if (!isDirty()) return;
		monitor.beginTask("Save "+getEditorInput().getName(), 10);
		try {
			for (IVariable var : this.variables) {
		        if (var.getErrorMessage()!=null) {
		        	MessageDialog.openError(getSite().getShell(), "Invalid XPath",
		        			"The variable '"+var.getVariableName()+"' is invalid.\n\n"+var.getErrorMessage());
		        	return;
		        }
			}
				
			final Properties props = new Properties();
			for (IVariable var : this.variables) {
				if (var==null) continue;
				final String name = var.getVariableName();
				final String val  = var instanceof XPathVariable
				                  ? ((XPathVariable)var).getSaveString()
				                  : "";
				props.setProperty(name, val);
			}
			
			try {
				PropUtils.storeProperties(props, EclipseUtils.getFile(getEditorInput()));
				isDirty = false;
				XPathEditor.this.firePropertyChange(PROP_DIRTY);
				EclipseUtils.getIFile(getEditorInput()).refreshLocal(IResource.DEPTH_ONE, monitor);
			} catch (Exception e) {
				logger.error("Cannot save "+EclipseUtils.getFile(getEditorInput()), e);
			}
			
		} finally {
			monitor.done();
		}
		
	}


	@Override
	public void doSaveAs() {
		
	}


	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}


	@Override
	public boolean isDirty() {
		return isDirty;
	}


	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void addVariable() {
		
		final String    name   = VariableUtils.getUniqueVariableName("new_xpath", variables);
		final IVariable newVar = new XPathVariable(name, "/");
		
		final IVariable var = getSelected();
		final int     index = var!=null ? variables.indexOf(var) : variables.size()-1;
		this.variables.add(index+1, newVar);
		viewer.refresh();
		viewer.editElement(newVar, 0);
		isDirty = true;
		XPathEditor.this.firePropertyChange(PROP_DIRTY);
		updateActions();
	}

	public void deleteVariable() {
		final IVariable var = getSelected();
		if (var==null) return;
		variables.remove(var);
		viewer.refresh();
		isDirty = true;
		XPathEditor.this.firePropertyChange(PROP_DIRTY);
		updateActions();
	}

	public void revert() {
		if (variables!=null) variables.clear();
		variables = null;
		if (upstream!=null) upstream.clear();
		upstream = null;
		createActorContentProvider();
		viewer.refresh();
	}

	public IVariable getSelected() {
		return (IVariable)((StructuredSelection)viewer.getSelection()).getFirstElement();
	}


}
