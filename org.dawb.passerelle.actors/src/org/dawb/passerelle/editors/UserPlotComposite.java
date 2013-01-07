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

import org.dawb.common.ui.plot.IPlottingSystem;
import org.dawb.common.ui.plot.PlottingFactory;
import org.dawb.passerelle.actors.Activator;
import org.dawb.workbench.jmx.IRemoteWorkbenchPart;
import org.dawb.workbench.jmx.UserPlotBean;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserPlotComposite implements IRemoteWorkbenchPart {

	private static Logger logger = LoggerFactory.getLogger(UserPlotComposite.class);
	
	private String                     partName;
	private Closeable                  closeable;
	private Queue<Object>              queue;
	private UserPlotBean               userPlotBean, originalUserPlotBean;
	private IPlottingSystem            system;

	public UserPlotComposite() {
		
	}
	
	@Override
	public void createRemotePart(final Object container, Closeable closeable) {
		
		final Composite contents = new Composite((Composite)container, SWT.NONE);
		this.closeable = closeable;
		contents.setLayout(new GridLayout(1, false));
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Create plot and add empty tool ready for tool pages to work
		// directly on this 
		try {
			system = PlottingFactory.createPlottingSystem();
		} catch (Exception e) {
			logger.error("!", e);
		}
		
		// TODO 
	}
	

	private void createPlot(UserPlotBean bean) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Create the actions.
	 */
	public void initializeMenu(Object bars) {
		MenuManager man = new MenuManager();
		man.add(confirm);
		man.add(stop);
	}
	
	@Override
	public void setConfiguration(String configurationXML) throws Exception {
		throw new RuntimeException(getClass().getName()+" does not support configuration xml yet!");
	}

	
	public boolean setFocus() {
		// TODO 
		return false;
	}

	public void dispose() {
			
		if (queue!=null) {
			queue.clear();
			if (queue!=null) {
				// Just in case something is waiting
				// An empty one cancels the message.
				if (queue.isEmpty()) queue.add(new UserPlotBean());
			}
		}
		this.queue          = null;
	}

	/**
	 * Queue must not be null and is cleared prior to using.
	 */
	public void setQueue(Queue<Object> queue) {
		this.queue = queue;
		queue.clear();
	}
	
	public void setValues(final Map<String, String> inputValues) {
		throw new RuntimeException("Set values as a Map of strings it not supported for Plotting!");
	}

	// Actions used by class
	protected final Action confirm = new Action("Confirm values, close view and continue workflow.", Activator.getImageDescriptor("icons/application_form_confirm.png")) {
		public void run() {
			doConfirm();
			closeable.close();
		}
	};
	
	// Actions used by class
	protected final Action stop = new Action("Stop workflow downstream of this node.", Activator.getImageDescriptor("icons/stop_workflow.gif")) {
		public void run() {
			doStop();
			closeable.close();
		}
	};
	

	public String getPartName() {
		return partName;
	}

	protected void doConfirm() {
		if (queue==null || userPlotBean==null) {
			MessageDialog.open(MessageDialog.INFORMATION, Display.getCurrent().getActiveShell(),
					           "Cannot confirm", "The workflow is not waiting for you to confirm these values.\n\nThere is currently nothing to confirm.", SWT.NONE);
			return;
		}
		if (queue.isEmpty()) queue.add(userPlotBean);
	}
	protected void doStop() {
		if (queue.isEmpty()) queue.add(new UserPlotBean());
	}

	public void setPartName(String partName) {
		this.partName = partName;
	}

	@Override
	public void setUserObject(Object userObject) {
		this.originalUserPlotBean = (UserPlotBean)userObject;
		createPlot(originalUserPlotBean);
	}


	@Override
	public void stop() {
		this.stop.run();
	}


	@Override
	public void confirm() {
		this.confirm.run();
	}


	@Override
	public Object getViewer() {
		return system;
	}
	@Override
	public boolean isMessageOnly() {
		return false;
	}
	@Override
	public void setRemoteFocus() {
		setFocus();
	}

}
