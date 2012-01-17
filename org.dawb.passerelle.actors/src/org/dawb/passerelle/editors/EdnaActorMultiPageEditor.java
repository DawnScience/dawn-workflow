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

import java.util.Properties;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.passerelle.actors.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdnaActorMultiPageEditor extends MultiPageEditorPart {

	private static final Logger logger = LoggerFactory.getLogger(EdnaActorMultiPageEditor.class);
	
	public static final String ID = "org.dawb.passerelle.editors.ednaEditor"; //$NON-NLS-1$
	
	private ISubstitutionEditor inputEditor;
	private XPathEditor outputEditor;
	private StructuredTextEditor xsdEditor;
	private StructuredTextEditor commonEditor;
	private Properties           linkerProps;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		
        super.init(site, input);
        
        try {
        	this.linkerProps = new Properties();
        	linkerProps.load(EclipseUtils.getIFile(getEditorInput()).getContents());
        	
        	setPartName(linkerProps.getProperty("org.dawb.edna.name"));
        	
        } catch (Exception e) {
        	logger.error("Cannot read linker properties "+getClass().getName()+"!", e);
        }
    }

	@Override
	protected void createPages() {
		try {
				
			final IFile        inputFile = (IFile)ResourcesPlugin.getWorkspace().getRoot().findMember(linkerProps.getProperty("org.dawb.edna.input"));
			final IEditorInput input     = new FileEditorInput(inputFile);

			this.inputEditor = new XMLSubstitutionEditor(linkerProps.getProperty("org.dawb.edna.name"),
					                                    linkerProps.getProperty("org.dawb.edna.moml"));
			addPage(0, inputEditor, input);
			setPageText(0, "Input");
			setPageImage(0, Activator.getImageDescriptor("icons/edna_input.gif").createImage());

			/**
			 * Important use StructuredTextEditor and set .moml as an xml file
			 * using the eclipse content type extension point.
			 */
			
			final IFile        outputFile = (IFile)ResourcesPlugin.getWorkspace().getRoot().findMember(linkerProps.getProperty("org.dawb.edna.output"));
			final IEditorInput output     = new FileEditorInput(outputFile);

			this.outputEditor = new XPathEditor(linkerProps.getProperty("org.dawb.edna.name"),
                                                          linkerProps.getProperty("org.dawb.edna.moml"));
			addPage(1, outputEditor, output);
			setPageText(1, "Output");
			setPageImage(1, Activator.getImageDescriptor("icons/edna_output.gif").createImage());

			/**
			 * Important use StructuredTextEditor and set .moml as an xml file
			 * using the eclipse content type extension point.
			 */
//			TODO: Fix the output and schema for EDNA data model files
//			final IFile        schemaFile = (IFile)ResourcesPlugin.getWorkspace().getRoot().findMember(linkerProps.getProperty("org.dawb.edna.schema"));
//			final IEditorInput schema     = new FileEditorInput(schemaFile);
//
//			this.xsdEditor = new StructuredTextEditor();
//			addPage(2, xsdEditor, schema);
//			setPageText(2, "XSD");
//			setPageImage(2, Activator.getImageDescriptor("icons/edna_xsd.png").createImage());
//			
//			final IFile        commonFile = (IFile)ResourcesPlugin.getWorkspace().getRoot().findMember("/edna/kernel/datamodel/XSDataCommon.xsd");
//			final IEditorInput common     = new FileEditorInput(commonFile);
//
//			this.commonEditor = new StructuredTextEditor();
//			addPage(3, commonEditor, common);
//			setPageText(3, "Common");
//			setPageImage(3, Activator.getImageDescriptor("icons/edna_common.png").createImage());

		} catch (Exception e) {
			logger.error("Cannot initiate "+getClass().getName()+"!", e);
		}
	}


	/** 
	 * No Save
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		final int pages = getPageCount();
		for (int i = 0; i < pages; i++) {
			final IFile file = EclipseUtils.getIFile(getEditor(i).getEditorInput());
			if (!file.isReadOnly()) getEditor(i).doSave(monitor);
		}
	}

	/** 
	 * No Save
	 */
	@Override
	public void doSaveAs() {
	}

	/** 
	 * We are not saving this class
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	public boolean isDirty() {
		final int pages = getPageCount();
		for (int i = 0; i < pages; i++) {
			if (getEditor(i).isDirty()) return true;
		}
        return false;
	}

	public ISubstitutionEditor getEdnaActorInputEditor() {
		return this.inputEditor;
	}
	public XPathEditor getEdnaActorOutputEditor() {
		return this.outputEditor;
	}
}
