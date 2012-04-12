/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.dawb.common.util.SubstituteUtils;
import org.dawb.common.util.io.FileUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawb.passerelle.editors.SubstitutionEditor;
import org.dawb.passerelle.editors.SubstitutionParticipant;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.workbench.model.actor.IPartListenerActor;
import com.isencia.passerelle.workbench.model.actor.IResourceActor;
import com.isencia.passerelle.workbench.model.actor.ResourceObject;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;
import com.isencia.passerelle.workbench.util.ResourceUtils;

/**
 * A actor for replacing variables in a file and writing it somewhere.
 * 
 * @author gerring
 *
 */
public class SubstituteTransformer extends AbstractDataMessageTransformer implements SubstitutionParticipant, IResourceActor, IPartListenerActor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5382424251791664468L;
	
	private static Logger logger = LoggerFactory.getLogger(SubstituteTransformer.class);
	
	private ResourceParameter templateParam, outputParam;
	private StringParameter   encoding;

	public SubstituteTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		this.templateParam = new ResourceParameter(this, "Template Source");
		registerConfigurableParameter(templateParam);
		
		this.outputParam = new ResourceParameter(this, "Output");
		outputParam.setResourceType(IResource.FOLDER);
		outputParam.setExpression("/${project_name}/output/");
		registerConfigurableParameter(outputParam);
		
		this.encoding = new StringParameter(this, "Encoding") {
			private static final long serialVersionUID = -1902977727142062610L;
			public String[] getChoices() {
				return new String[]{"US-ASCII", "ISO-8859-1", "UTF-8" ,"UTF-16BE","UTF-16LE" ,"UTF-16"};
			}
		};
		encoding.setExpression("UTF-8");
		registerConfigurableParameter(encoding);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(final List<DataMessageComponent> cache) throws ProcessingException {
		

		String fromPath   = null;
		String outputPath = null;
		try {
		    fromPath   = ParameterUtils.getSubstituedValue(templateParam, cache);
		    outputPath = ParameterUtils.getSubstituedValue(outputParam, cache);
		} catch (Exception e) {
			throw createDataMessageException("Cannot substitute parameter of "+templateParam.getDisplayName()+" and/or "+outputParam.getDisplayName()+" in "+getDisplayName(), e);
		}
		
		// Copy file to new location
		final IFile    input = (IFile)ResourceUtils.getResource(fromPath);
		final IResource  res = ResourceUtils.getResource(outputPath);
		
		IFile output = null;
		if (res instanceof IContainer) {
			if (res instanceof IProject) {
				output = ((IProject)res).getFile(input.getName());
			} else if (res instanceof IFolder) {
				output = ((IFolder)res).getFile(input.getName());
			}
		} else if (output instanceof IFile) {
			output = (IFile)res;
		}
		
		try {
			if (!output.exists()) {
			    output.create(input.getContents(), true, new NullProgressMonitor());
			} else{
				output.setContents(input.getContents(), true, false, new NullProgressMonitor());
			}
		} catch (Exception e) {
			throw createDataMessageException("Cannot read file "+fromPath+" and/or write file "+outputPath, e);
		}
		
		try {
    		final String rep = SubstituteUtils.substitute(output.getContents(), MessageUtils.getScalar(cache));

    		output.setContents(new ByteArrayInputStream(rep.getBytes(encoding.getExpression())), true, false, new NullProgressMonitor());
    		
		} catch (Exception e) {
			throw createDataMessageException("Cannot substitute "+output, e);
		}
		
		final DataMessageComponent comp = MessageUtils.mergeAll(cache);
		comp.putScalar("substitute_output", output.getLocation().toOSString());
		
		return comp;
	}

	@Override
	public List<IVariable> getOutputVariables() {
		
		final List<IVariable> ret = super.getOutputVariables();
		ret.add(new Variable("substitute_output", VARIABLE_TYPE.PATH, outputParam.getExpression(), String.class));
        return ret;
	}
	
	@Override
	protected String getOperationName() {
		return "Substitute file with variables.";
	}

	@Override
	public void setMomlResource(IResource momlFile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getResourceCount() {
		return 1;
	}

	@Override
	public ResourceObject getResource(int iresource) throws Exception {
		
		if (iresource==0) {
			final ResourceObject ret = new ResourceObject();
			
			ret.setResource(getResource());
			ret.setResourceTypeName("Substitution Editor");
			ret.setEditorId(SubstitutionEditor.ID);
			return ret;
		} 
		
		return null;
	}

	private IFile getResource() throws Exception {
		final String tempPath = ParameterUtils.substitute(templateParam.getExpression(), this);
		final IFile  file     = (IFile)ResourceUtils.getResource(tempPath);
		return file;
	}

	@Override
	public String getDefaultSubstitution() {
		
		if (templateParam.getExpression()==null) return null;
		StringBuffer buf = null;
		try {
			buf = FileUtils.readFile(new File(templateParam.getExpression()));
		} catch (Exception e) {
			return "";
		}
	    return buf!=null ? buf.toString() : "";
	}

	@Override
	public void partPreopen(ResourceObject ob) {
		
		try {
			final IFile file = (IFile)getResource();
			if (!file.exists()) {
	        	final InputStream is = new ByteArrayInputStream(getDefaultSubstitution().getBytes("UTF-8"));
	        	
	        	if (!file.getParent().exists() && file.getParent() instanceof IFolder) {
	        		IFolder par = (IFolder)file.getParent();
	        		par.create(true, true, new NullProgressMonitor());
	        	}
	        	
	        	file.create(is, true, new NullProgressMonitor());
	        	file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			}
		} catch (Exception ne) {
			logger.error("Cannot create file "+ob.getResourceTypeName(), ne);
		}
	}

	@Override
	public void partOpened(IWorkbenchPart part, ResourceObject ob) {
		final SubstitutionEditor ed = (SubstitutionEditor)part;
		ed.setSubstitutionParticipant(this);
		part.setFocus();
	}
}
