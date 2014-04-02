/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.actors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dawb.common.util.io.IFileUtils;
import org.dawb.passerelle.common.actors.IDescriptionProvider.Requirement;
import org.dawb.passerelle.common.actors.IDescriptionProvider.VariableHandling;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.gmf.runtime.common.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.workbench.model.actor.IResourceActor;
import com.isencia.passerelle.workbench.model.actor.ResourceObject;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;

public abstract class AbstractScriptTransformer extends AbstractPassModeTransformer implements IResourceActor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 780021329637752897L;

	/**
	 * 
	 */
	private static Logger logger = LoggerFactory.getLogger(AbstractScriptTransformer.class);
	
	public ResourceParameter   scriptFileParam;
	protected String              scriptFilePath;
	
	private final List<DataMessageComponent> cache;

	public AbstractScriptTransformer(CompositeEntity container, String name) throws Exception {
		
		super(container, ModelUtils.findUniqueActorName(container, name));
		cache = new ArrayList<DataMessageComponent>(7);
		
		scriptFileParam = getScriptParameter((Actor)this);
		registerConfigurableParameter(scriptFileParam);
				
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
	}

	/**
	 * returns the parameter for the script file. Also 
	 * sets a default location for the script.
	 * 
	 * @param actor
	 * @return
	 * @throws Exception
	 */
	protected abstract ResourceParameter getScriptParameter(Actor actor) throws Exception;

	/**
	 * 
	 * @param cache
	 * @return
	 * @throws Exception
	 */
	protected abstract DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws Exception;

	
	private DataMessageComponent getTransformedMessageInternal(List<DataMessageComponent> cache) throws Exception {
		
		if (cache==null||cache.isEmpty()) return null;
		try {
			ActorUtils.setActorExecuting(this, true);

			DataMessageComponent ret = getTransformedMessage(cache);
			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(cache), ret);
				if (bean!=null) bean.setPortName(input.getDisplayName());
				ActorUtils.debug(this, bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}

			return ret;
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}
	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		super.attributeChanged(attribute);            

	}
	
	@Override
	protected void process(ActorContext ctxt, ProcessRequest request,
			ProcessResponse response) throws ProcessingException {
		ManagedMessage message = request.getMessage(input);
		try {
			if (message!=null) {
				DataMessageComponent msg = MessageUtils.coerceMessage(message);
				cache.add(msg);
			}
			
			final DataMessageComponent despatch = getTransformedMessageInternal(cache);
			if (despatch==null) return;
	    
			response.addOutputMessage(output, MessageUtils.getDataMessage(despatch, message));
		} catch (DataMessageException dme) {
			throw dme;
		} catch (ProcessingException pe) {
			throw new ProcessingException(pe.getErrorCode(), pe.getMessage(), pe.getModelElement(), message, pe.getCause());
		} catch (Exception ne) {
			throw createDataMessageException("Cannot add data from '"+message+"'", ne);
		}
	}

	protected IResource getResource() throws Exception {
		
		String path = scriptFileParam.stringValue();
		IFile  file = null;
		if (path==null || "".equals(path)) {
			// Resource doesn't exist - create new file
			final IProject          project= getProject();
			final IContainer        src    = project.getFolder("src");
			file = IFileUtils.getUniqueIFile(src, "python_script", "py");
			path   = file.getFullPath().toOSString();
			path   = StringUtil.replace(path, "/"+file.getProject().getName()+"/", "/${project_name}/", true);
			scriptFileParam.setExpression(path);
		} else {
			if (ResourcesPlugin.getWorkspace().getRoot().findMember(path) != null)
				if (ResourcesPlugin.getWorkspace().getRoot().findMember(path).exists()) {
					// Resource exists in workspace
					scriptFileParam.setExpression(path);
					file = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
				}
		}
		if (file==null) {
			// Path exists but file is null...
			path = ParameterUtils.substitute(path, this);
			final IProject project= getProject();
			final String     srcP = IFileUtils.getPathWithoutProject(path.substring(0,path.lastIndexOf('/')));
			IContainer src  = (IContainer)project.findMember(srcP);
			if (src==null) {
				try {
					IFolder srcf = project.getFolder(srcP);
					srcf.create(true, true, new NullProgressMonitor());
					src = srcf;
				} catch (Exception ne) {
					logger.error("Cannot create folder "+srcP, ne);
				}
			}
			final String  fileName= path.substring(path.lastIndexOf('/'));
			file = (IFile)src.findMember(fileName);
			if (file==null&&src instanceof IProject) {
				file = ((IProject)src).getFile(fileName);
			} if (file==null&&src instanceof IFolder) {
				file = ((IFolder)src).getFile(fileName);
			}
		}
		
		try {
			if (!file.exists()) {
	        	final InputStream is = new ByteArrayInputStream(createScript().getBytes("UTF-8"));
	        	file.create(is, true, new NullProgressMonitor());
	        	file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			}
		} catch (Exception ne) {
			logger.error("Cannot create file "+path, ne);
		}
		
		return file;
	}

	protected abstract String createScript();

	protected String getResourceTypeName() throws Exception {
		final String path = getResource().getName();
		if (path==null) return "";
		return "'"+getResource().getName()+"'";
	}
	
	@Override
	public int getResourceCount() {
		return 1;
	}
	
	@Override
	public ResourceObject getResource(int num) throws Exception {
		if (num==0) {
			final ResourceObject ret = new ResourceObject();
			ret.setResource(getResource());
			ret.setResourceTypeName(getResourceTypeName());
			return ret;
		}
		return null;
	}
	
	protected DataMessageException createDataMessageException(String msg,Throwable e) throws DataMessageException {
		return new DataMessageException(msg, this, cache, e);
	}
	
	
	protected void setUpstreamValues(final DataMessageComponent ret,
			                         final List<DataMessageComponent> cache) {
		
		ret.setMeta(MessageUtils.getMeta(cache));
		ret.addScalar(MessageUtils.getScalar(cache));
	}
}
