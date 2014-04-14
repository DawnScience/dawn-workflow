/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (moml).
 */

public class PasserelleNewProjectWizardPage extends WizardPage {

	private Text projectName;
	private Button examples;
	private ISelection selection;
	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public PasserelleNewProjectWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Create new workflow project");
		setDescription("A project marked as both a workflow and python development project.");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		Label lblprojectName = new Label(container, SWT.NULL);
		lblprojectName.setText("&Project name:");

		projectName = new Text(container, SWT.BORDER | SWT.SINGLE);
		projectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		projectName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		
		Label lblExamples = new Label(container, SWT.NULL);
		lblExamples.setText("&Create example data");

		examples = new Button(container, SWT.CHECK);
		examples.setSelection(true);

		dialogChanged();
		setControl(container);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {

	}


	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		
		final String projectName = getProjectName();
		if (projectName == null || "".equals(projectName)) {
			updateStatus("Please set the project name");
			return;
		}
		final IProject project   = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project!=null && project.exists()) {
			updateStatus("Please set a unique name, '"+projectName+"' already exists");
			return;
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getProjectName() {
		if (projectName==null||projectName.isDisposed()) return null;
		return projectName.getText();
	}

	public boolean isExamples() {
		if (examples==null||examples.isDisposed()) return false;
		return examples.getSelection();
	}
}
