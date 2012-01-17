/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.perspective;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.common.Activator;
import org.dawb.passerelle.common.preferences.PreferenceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isencia.passerelle.workbench.model.ui.IPasserelleMultiPageEditor;

public class WorkflowChecking implements IStartup {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowChecking.class);
	
	@Override
	public void earlyStartup() {
		createWorkflowsPerspectiveListener();
	}
	private void createWorkflowsPerspectiveListener() {

		final IWorkbenchPage page = EclipseUtils.getPage();
		if (page!=null) {
			page.addPartListener(new IPartListener() {
				
				@Override
				public void partOpened(IWorkbenchPart part) {
					checkWorkflowPerspective(part);
				}

				private void checkWorkflowPerspective(IWorkbenchPart part) {
					if (part instanceof IPasserelleMultiPageEditor) {

						final String id = page.getPerspective().getId();
						if (!id.equals(WorkflowPerspective.ID)) {

							final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

							final String setting = store.getString(PreferenceConstants.REMEMBER_WORKFLOWS_PERSPECTIVE);
							boolean openWorkflows= false;
							if (setting.equals(MessageDialogWithToggle.PROMPT) && System.getProperty("org.dawb.test.session")==null) {
								MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(page.getWorkbenchWindow().getShell(), 
										"Open Workflow Perspective", "This kind of file is associated with the 'Workflow' Perspective.\n\nWould you like to switch to the workflow perspective now?", 
										"Remember my decision", false, 
										store, PreferenceConstants.REMEMBER_WORKFLOWS_PERSPECTIVE);

								switch (dialog.getReturnCode()) {
								case IDialogConstants.CANCEL_ID:
									return;
								case IDialogConstants.YES_ID:
									openWorkflows = true;
									break;
								case IDialogConstants.NO_ID:
									openWorkflows = false;
									break;
								}
							} else if (setting.equals(MessageDialogWithToggle.ALWAYS)) {
								openWorkflows = true;
							}
							
							if (openWorkflows) {
								try {
									PlatformUI.getWorkbench().showPerspective(WorkflowPerspective.ID, page.getWorkbenchWindow());
								} catch (Exception ne) {
									logger.error(ne.getMessage(), ne);
								}
							}
						}
					}
				}

				@Override
				public void partDeactivated(IWorkbenchPart part) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void partClosed(IWorkbenchPart part) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void partBroughtToTop(IWorkbenchPart part) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void partActivated(IWorkbenchPart part) {
					// TODO Auto-generated method stub
					
				}
			});
		};
	}

}
