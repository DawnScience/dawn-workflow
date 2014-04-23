/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.ui.editors;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.passerelle.common.editor.ISubstitutionEditor;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.SubstitutionParticipant;
import org.dawb.passerelle.common.project.PasserelleProjectUtils;
import org.dawb.passerelle.common.utils.SubstituteUtils;
import org.dawb.passerelle.ui.Activator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.editors.text.TextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.CompositeActor;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLParser;

/**
 * TODO Same as XMLSubstitutionEditor, copied a lot. Use delegation to fix.
 * @author gerring
 *
 */
public class SubstitutionEditor extends TextEditor implements ISubstitutionEditor{
	
    public static final String ID = "org.dawb.passerelle.editors.substitutionEditor";

	private static Logger logger = LoggerFactory.getLogger(SubstitutionEditor.class);
	
	private String                  momlPath;
	private String                  actorName;
	private TableViewer             viewer;
	private SubstitutionParticipant actor;
	private CLabel                  messageLabel;
	private TextSelection           currentSelectedText;
	
	public SubstitutionEditor() {
	}
	
	public void setSubstitutionParticipant(SubstitutionParticipant actor) {
		this.actor = actor;
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
		messageLabel.setToolTipText("Insert variables to the template xml on the right,\nthis will be replaced and sent to EDNA when the workflow is run.");
		
		ToolBarManager toolMan = new ToolBarManager(SWT.FLAT|SWT.LEFT);
		final ToolBar          toolBar = toolMan.createControl(tools);
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		EdnaActorActions.createInputActions(toolMan);
      
        final SashForm  sash = new SashForm(main, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.viewer = new TableViewer(sash, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        createColumns(viewer);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		
        final Composite right = new Composite(sash, SWT.NONE);
        right.setLayout(new FillLayout());
        super.createPartControl(right);
        
		sash.setWeights(new int[]{20,80});
		
		// Attached a painter to highlight the replacements
		final VariableCharacterMatcher matcher = new VariableCharacterMatcher(((SourceViewer)getSourceViewer()));
		final VariablePainter          painter = new VariablePainter(getSourceViewer(), matcher);
		((SourceViewer)getSourceViewer()).addPainter(painter);
		viewer.addSelectionChangedListener(matcher);
		viewer.addDoubleClickListener(new IDoubleClickListener() {		
			@Override
			public void doubleClick(DoubleClickEvent event) {
				doInsert();
			}
		});
		((SourceViewer)getSourceViewer()).addSelectionChangedListener(new ISelectionChangedListener() {		
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final ISelection sel  = event.getSelection();
				if (sel instanceof TextSelection) {
					SubstitutionEditor.this.currentSelectedText = (TextSelection)sel;
					updateMessageLabel();
					return;
				}
				SubstitutionEditor.this.currentSelectedText = null;
				updateMessageLabel();
			}
		});
		
		toolMan.update(true);
		setWritable(true);
		getSite().setSelectionProvider(viewer);
		createUndoRedoActions();
		createRightClickMenu();
 	}
	
	private void createRightClickMenu() {
	    final MenuManager menuManager = new MenuManager();
	    viewer.getControl().setMenu (menuManager.createContextMenu(viewer.getControl()));
		EdnaActorActions.createInputActions(menuManager);
	}
	
	private Image editableImage, uneditableImage;
	
	protected void setWritable(boolean isWritable) {
		
		if (editableImage==null)   editableImage = Activator.getImageDescriptor("icons/editable.png").createImage();
		if (uneditableImage==null) uneditableImage = Activator.getImageDescriptor("icons/uneditable.png").createImage();
		
		getTextViewer().setEditable(isWritable);
		viewer.getTable().setEnabled(isWritable);
		updateMessageLabel(isWritable);
	}
	
	protected SourceViewer getTextViewer() {
		return (SourceViewer)getSourceViewer();
	}
	
    private void updateMessageLabel() {
    	updateMessageLabel(viewer.getTable().isEnabled());
    }
	private void updateMessageLabel(boolean isWritable) {
		
		if (isWritable) {
			if (currentSelectedText!=null) { 
				messageLabel.setImage(editableImage);
				messageLabel.setText("Double click to insert.");
			} else {
				messageLabel.setImage(editableImage);
				messageLabel.setText("Select some text.");
			}
		} else {
			messageLabel.setImage(uneditableImage);
			messageLabel.setText("Uneditable.");
		}
		
		messageLabel.getParent().layout(new Control[]{messageLabel});
	}

	private void doInsert() {

		try {
			final StructuredSelection sel = (StructuredSelection)viewer.getSelection();
			if (sel == null) return;
			final IVariable           var = (IVariable)sel.getFirstElement();
			final IDocument           doc = getTextViewer().getDocument();
			final String              rep = "${"+var.getVariableName()+"}";
			
			if (currentSelectedText!=null) {
				final int total = currentSelectedText.getOffset()+currentSelectedText.getLength();
				int       len   = currentSelectedText.getLength();
				final int tLen  = doc.getLength();
				if (total>tLen) len = tLen-currentSelectedText.getOffset();
				if (len<0) len = 0;
				doc.replace(currentSelectedText.getOffset(), len, rep);
			}
		} catch (BadLocationException e) {
			logger.error("Cannot replace selection "+getTextViewer().getSelectedRange(), e);
		}

	}
	
	public void doSave(IProgressMonitor mon) {
		if (!viewer.getTable().isEnabled()) {
			EdnaActorActions.showVars.setChecked(true);
			setPreview(false);
		}
		super.doSave(mon);
	}

	@Override
	public void setFocus() {
		
		super.setFocus();
		
		MoMLParser.purgeAllModelRecords();
		
		if (actor==null && momlPath!=null) {
			final MoMLParser parser = new MoMLParser(new Workspace());
			try {
				final IResource   moml  = ResourcesPlugin.getWorkspace().getRoot().findMember(momlPath);
				CompositeActor toplevel = (CompositeActor) parser.parse(null, new File(moml.getLocation().toString()).toURL());
				toplevel.workspace().setName(moml.getProject().getName());
				ComponentEntity entity  = PasserelleProjectUtils.findEntityByName(toplevel, actorName);
	
				if (entity!=null && entity.getName().equals(actorName)) {
					this.actor = (SubstitutionParticipant)entity;	
				}
	
			} catch (Exception e) {
				logger.error("Cannot parse "+momlPath, e);
			}
		}
		
		if (actor!=null) {
			viewer.setContentProvider(createActorContentProvider());
			viewer.setInput(new Object());
		}
	}

	private IContentProvider createActorContentProvider() {
		return new IStructuredContentProvider() {
			@Override
			public void dispose() {
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public Object[] getElements(Object inputElement) {
				if (actor==null) return new Object[]{"-"};
				final List<IVariable> vars = actor.getInputVariables();
				if (vars!=null&&!vars.isEmpty()) return vars.toArray();
				return new Object[]{"-"};
			}
		};
	}

	private void createColumns(final TableViewer viewer) {
		
		ColumnViewerToolTipSupport.enableFor(viewer,ToolTip.NO_RECREATE);

        TableViewerColumn var   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		var.getColumn().setText("Name");
		var.getColumn().setWidth(200);
		var.setLabelProvider(new VariableLabelProvider(0));
	}
	
    public void dispose() {
    	super.dispose();
    	viewer.getTable().dispose();
    	if (editableImage!=null)   editableImage.dispose();
    	if (uneditableImage!=null) uneditableImage.dispose();
    }

	public void revert() {
		if (actor != null) {
			setWritable(true);
			getTextViewer().getDocument().set(actor.getDefaultSubstitution());
		}
	}
	
	public void setText(String rep, boolean writable) {

		setWritable(writable);
		getTextViewer().getDocument().set(rep);
		getTextViewer().refresh();
		//update();
	}

	public String getText() {
		return getTextViewer().getDocument().get();
	}

	private String replaceText;

	public String getReplaceText() {
		return replaceText;
	}

	public void setReplaceText(String replaceText) {
		this.replaceText = replaceText;
	}

	public SubstitutionParticipant getActor() {
		return actor;
	}


	public void setPreview(boolean isPreview) {
		if (!isPreview) {
			final String text = getReplaceText();
			if (text == null) return;
    		setText(text, true);
    		setReplaceText(null);
		} else {
    		EclipseUtils.getActivePage().saveEditor(this, false);
    		final Map<String,String>    vars = actor.getExampleValues();
    		
    		final String text   = getText();
    		setReplaceText(text);
    		final String rep    = SubstituteUtils.substitute(text, vars);
    		
    		setText(rep, false);
			
		}
		
	}


}
