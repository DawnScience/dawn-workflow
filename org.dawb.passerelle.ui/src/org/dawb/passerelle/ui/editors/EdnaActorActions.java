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

import org.dawb.common.ui.menu.CheckableActionGroup;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.common.editor.ISubstitutionEditor;
import org.dawb.passerelle.ui.Activator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdnaActorActions {

	private static final Logger logger = LoggerFactory.getLogger(EdnaActorActions.class);

	public static final Action showVars, showSubstitution, revertInput, 
	                           addXPath, deleteXPath, revertOutput;
	
	/**
	 * We have the actions defined statcially but added to several IContributionManager
	 */
	static {
        showVars = new Action("Show variables", IAction.AS_CHECK_BOX) {
        	@Override
        	public void run() {
    			final ISubstitutionEditor ednaEditor = getEdnaInputEditor();
    			ednaEditor.setPreview(false);
    			showVars.setChecked(true);
        	}
		};
		showVars.setImageDescriptor(Activator.getImageDescriptor("icons/xml_variables.png"));
		showVars.setChecked(true);
		
		showSubstitution = new Action("Preview substitution", IAction.AS_CHECK_BOX) {
        	@Override
        	public void run() {
        		try {
        			final ISubstitutionEditor ednaEditor = getEdnaInputEditor();
	        		ednaEditor.setPreview(true);
	        		showSubstitution.setChecked(true);
        		} catch (Exception ne) {
        			logger.error("Cannot parse document to show values", ne);
        		}
        	}
		};
		showSubstitution.setImageDescriptor(Activator.getImageDescriptor("icons/xml_substitute.gif"));
		showSubstitution.setToolTipText("Shows estimation of file after a replace. Note if you save in this mode, it will revert back to variable mode to avoid saving the preview.");
			
		revertInput = new Action("Revert", IAction.AS_PUSH_BUTTON) {
        	@Override
        	public void run() {
    			final ISubstitutionEditor ednaEditor = getEdnaInputEditor();
    			showVars.setChecked(true);
    			ednaEditor.revert();
        	}
		};
		revertInput.setImageDescriptor(Activator.getImageDescriptor("icons/xml_revert.png"));
		revertInput.setToolTipText("Revert editor to default input");

		addXPath = new Action("Add xpath", IAction.AS_PUSH_BUTTON) {
        	@Override
        	public void run() {
        		try {
        			final XPathEditor ednaEditor = getEdnaOutputEditor();
        			ednaEditor.addVariable();		
        		} catch (Exception ne) {
        			logger.error("Cannot parse document to show values", ne);
        		}
        	}
		};
		addXPath.setToolTipText("Add variable defined by running an XPath query on the output.");
		addXPath.setImageDescriptor(Activator.getImageDescriptor("icons/xpath_add.png"));
		
		deleteXPath = new Action("Delete xpath", IAction.AS_PUSH_BUTTON) {
        	@Override
        	public void run() {
        		try {
        			final XPathEditor ednaEditor = getEdnaOutputEditor();
        			ednaEditor.deleteVariable();		
        		} catch (Exception ne) {
        			logger.error("Cannot parse document to show values", ne);
        		}
        	}
  
		};
		deleteXPath.setToolTipText("Delete variable defined by running an XPath query on the output.");
		deleteXPath.setImageDescriptor(Activator.getImageDescriptor("icons/xpath_delete.png"));

		revertOutput = new Action("Revert", IAction.AS_PUSH_BUTTON) {
        	@Override
        	public void run() {
    			final XPathEditor ednaEditor = getEdnaOutputEditor();
     			ednaEditor.revert();
        	}
		};
		revertOutput.setImageDescriptor(Activator.getImageDescriptor("icons/xml_revert.png"));
		revertOutput.setToolTipText("Revert editor to last saved value");

	}

	public static void createOutputActions(final IContributionManager toolMan) {
	 
		toolMan.add(addXPath);
		toolMan.add(deleteXPath);
		toolMan.add(new Separator(EdnaActorActions.class.getName()+".output.sep3"));
		toolMan.add(revertOutput);

	}
	
	public static void createInputActions(final IContributionManager toolMan) {
		
		final CheckableActionGroup group = new CheckableActionGroup();
		showVars.setChecked(true);
		group.add(showVars);
		toolMan.add(showVars);
		
		group.add(showSubstitution);
		toolMan.add(showSubstitution);
			
		
		toolMan.add(new Separator(EdnaActorActions.class.getName()+".input.sep1"));		
		toolMan.add(revertInput);

		toolMan.add(new Separator(EdnaActorActions.class.getName()+".input.sep2"));

	}
	
	private static final ISubstitutionEditor getEdnaInputEditor() {
		
		final IEditorPart            part = EclipseUtils.getActivePage().getActiveEditor();
		if (part==null) return null;
		
		if (part instanceof EdnaActorMultiPageEditor) {
			final EdnaActorMultiPageEditor ed = (EdnaActorMultiPageEditor)EclipseUtils.getActivePage().getActiveEditor();
			return ed.getEdnaActorInputEditor();
		}
		
		if (part instanceof ISubstitutionEditor) {
			return (ISubstitutionEditor)part;
		}
		
		return null;
	}
	
	private static final XPathEditor getEdnaOutputEditor() {
		
		final IWorkbenchPage         page = EclipseUtils.getActivePage();
		if (page==null) return null;
		final IEditorPart            part = page.getActiveEditor();
		if (part==null || !(part instanceof EdnaActorMultiPageEditor)) return null;
		final EdnaActorMultiPageEditor ed = (EdnaActorMultiPageEditor)part;
		return ed.getEdnaActorOutputEditor();
	}

}
