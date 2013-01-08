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

import java.util.Map;
import java.util.Queue;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawb.passerelle.actors.Activator;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart.Closeable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
/**
 * A editor that can deligate its UI to an implementation of IRemoteWorkbenchPart.
 * For instance user modify or user plot.
 * 
 * 
 * @author gerring
 *
 */
public class UserInputEditor extends EditorPart implements IRemoteWorkbenchPart, IRemoteWorkbenchPart.Closeable {
	
	public static final String ID = "org.dawb.passerelle.editors.UserModifyEditor"; //$NON-NLS-1$
	

	private Link                label;
	private IRemoteWorkbenchPart deligate;


	private Composite container;


	private ActionBarWrapper actionBarsWrapper;


	private ToolBar toolBar;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input); // Normally will be temporary file, not much use.
		setPartName("Review");
	}
	
	public void setPartName(final String partName) {
		super.setPartName(partName);
	}
	
	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		this.container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(container);
		container.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	    GridUtils.removeMargins(container);
	
		final Composite top    = new Composite(container, SWT.NONE);
		top.setLayout(new GridLayout(3, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        //GridUtils.removeMargins(top);
		
		this.label  = new Link(top, SWT.LEFT);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		//label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		label.setText("Please check and/or set values, then <a>continue</a> or <a>stop</a> the workflow.");
		label.addSelectionListener(new SelectionListener() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.text.contains("stop")) {
					deligate.stop();
				} else {
					deligate.confirm();
				}
				close();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		final MenuManager    menuMan = new MenuManager();
	    final ToolBarManager toolMan = new ToolBarManager(SWT.FLAT|SWT.RIGHT);
	    this.toolBar = toolMan.createControl(top);
	    toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
 
		final IActionBars bars = this.getEditorSite().getActionBars();
		this.actionBarsWrapper = new ActionBarWrapper(toolMan,menuMan,null,(IActionBars2)bars);

	}
	
	public void setDeligate(IRemoteWorkbenchPart part) {
		this.deligate = part;
		deligate.createRemotePart(container, this);
		
	
		deligate.initializeMenu(actionBarsWrapper);
		initializeToolBar(actionBarsWrapper);
		initializeMenu(actionBarsWrapper);
		
	    Action menuAction = new Action("", Activator.getImageDescriptor("/icons/DropDown.png")) {
	        @Override
	        public void run() {
                final Menu   mbar = ((MenuManager)actionBarsWrapper.getMenuManager()).createContextMenu(toolBar);
       		    mbar.setVisible(true);
	        }
	    };
	    actionBarsWrapper.getToolBarManager().add(menuAction);
	    actionBarsWrapper.getToolBarManager().update(true);

		getSite().setSelectionProvider((ColumnViewer)deligate.getViewer());

	}


	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar(IActionBars bars) {
//		IToolBarManager man = bars.getToolBarManager();
//		man.add(deligate.confirm);
//		man.add(deligate.stop);
//		man.add(new Separator(getClass().getName()+".sep2"));
//		man.add(deligate.add);
//		man.add(deligate.delete);
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu(IActionBars bars) {
//		IMenuManager man = bars.getMenuManager();
//		man.add(deligate.confirm);
//		man.add(deligate.stop);
//		man.add(new Separator(getClass().getName()+".sep3"));
//		man.add(deligate.add);
//		man.add(deligate.delete);
	}

	@Override
	public void setFocus() {
		if (deligate!=null) deligate.setRemoteFocus();
	}
	
	@Override
	public void setRemoteFocus() {
		setFocus();
	}

	public void dispose() {
		
		super.dispose();
		
		if (deligate!=null) deligate.dispose();
	}
	@Override
	public void doSave(IProgressMonitor monitor) {
		// Nothing to do
	}

	@Override
	public void doSaveAs() {
		// Nothing to do
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setQueue(Queue<Object> valueQueue) {
		deligate.setQueue(valueQueue);
	}

	@Override
	public void setValues(Map<String, String> values) {
		deligate.setValues(values);
		container.layout();
	}

	@Override
	public void setConfiguration(String configuration) throws Exception {
		deligate.setConfiguration(configuration);
	}


	@Override
	public boolean close() {
		return EclipseUtils.getActivePage().closeEditor(this, false);
	}
	@Override
	public void setUserObject(Object userObject) {
		deligate.setUserObject(userObject);
	}
	@Override
	public void stop() {
		deligate.stop();
	}

	@Override
	public void confirm() {
		deligate.confirm();
	}

	@Override
	public Object getViewer() {
		return deligate.getViewer();
	}

	@Override
	public boolean isMessageOnly() {
		return deligate.isMessageOnly();
	}

	@Override
	public void createRemotePart(Object container, Closeable closeable) {
		// We already called it for them.
	}

	@Override
	public void initializeMenu(Object iActionBars) {
		deligate.initializeMenu(iActionBars);
	}

}
