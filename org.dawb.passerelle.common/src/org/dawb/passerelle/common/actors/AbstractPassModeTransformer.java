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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.dawb.passerelle.common.message.AbstractDatasetProvider;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.IDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.IOPort;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.ChangeListener;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.resources.util.ResourceUtils;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;

public abstract class AbstractPassModeTransformer extends Actor implements IVariableProvider, IProjectNamedObject, IDescriptionProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPassModeTransformer.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -1903160956377231213L;

	protected static List<String> MEMORY_MODE;
	static {
		MEMORY_MODE = new ArrayList<String>(3);
		MEMORY_MODE.add("Create copy of data leaving original data intact.");
		MEMORY_MODE.add("Operate on data directly to save memory.");
	}
	
	protected static List<String> NAME_MODE;
	static {
		NAME_MODE = new ArrayList<String>(3);
		NAME_MODE.add("Attempt to use image name if there is one.");
		NAME_MODE.add("Leave as name based on previous nodes.");
		NAME_MODE.add("Use name of this actor as the data set name.");
	}
	
	public final StringChoiceParameter memoryManagementParam;
	protected String                      memoryMode;
	
	public final StringChoiceParameter dataSetNaming;
	protected String                      namingMode;
	
	public Port input;
	public Port output;
	
	
	/**
	 * Cached variables
	 */
	private ArrayList<IVariable> cachedUpstreamVariables;
    
	public AbstractPassModeTransformer(final CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, ResourceUtils.findUniqueActorName(container, name));
		
		input = PortFactory.getInstance().createInputPort(this, null);
		output = PortFactory.getInstance().createOutputPort(this);
		
		memoryManagementParam = new StringChoiceParameter(this, "Memory Mode", getMemoryModes(), 1 << 2);
		memoryManagementParam.setExpression(MEMORY_MODE.get(0));
		registerExpertParameter(memoryManagementParam);
		memoryMode = MEMORY_MODE.get(0);
		
		dataSetNaming = new StringChoiceParameter(this, "Name Mode", getNameModes(), 1 << 2);
		dataSetNaming.setExpression(NAME_MODE.get(0));
		registerExpertParameter(dataSetNaming);
		namingMode = NAME_MODE.get(0);
		
		this.cachedUpstreamVariables = new ArrayList<IVariable>(7);

		// Any change upsteam means they are invalid.
		container.addChangeListener(new ChangeListener() {		
			@Override
			public void changeFailed(ChangeRequest change, Exception exception) { }
			@Override
			public void changeExecuted(ChangeRequest change) {
				cachedUpstreamVariables.clear();
				if (!container.deepContains(AbstractPassModeTransformer.this)) {
					container.removeChangeListener(this);
				}
			}
		});
		
		
		ActorUtils.createDebugAttribute(this);
	}

	/**
	 * Override to provide different options for processing of
	 * memory.
	 * 
	 * @return
	 */
	protected List<String> getMemoryModes() {
		return MEMORY_MODE;
	}
	/**
	 * Override to provide different options for processing of
	 * names.
	 * 
	 * @return
	 */
	protected List<String> getNameModes() {
		return NAME_MODE;
	}
	
	
	protected boolean isCreateClone() {
		return MEMORY_MODE.get(0).equals(memoryMode);
	}
	
	
    /**
     * Adds the scalar and the 
     */
	@Override
	public List<IVariable> getOutputVariables() {
		
        final List<IVariable> ret = getScalarOutputVariables();
     
        return ret;
	}
	
    /**
     * By default just passes the upstream string and path variables as 
     * these are normally preserved
     */
	protected List<IVariable> getScalarOutputVariables() {

		final List<IVariable> ret = new ArrayList<IVariable>(7);
		final List<IVariable> all = getInputVariables();
		for (IVariable iVariable : all) {
			if (iVariable.getVariableType()==VARIABLE_TYPE.PATH || 
				iVariable.getVariableType()==VARIABLE_TYPE.SCALAR|| 
				iVariable.getVariableType()==VARIABLE_TYPE.XML) {
				ret.add(iVariable);
			}
		}
		ret.add(new Variable("project_name", VARIABLE_TYPE.SCALAR, getProject().getName()));
		
		if (dataSetNaming.getVisibility()!=Settable.NONE) {
			if (NAME_MODE.get(2).equals(dataSetNaming.getExpression())) {
				ret.add(new Variable(getName(), VARIABLE_TYPE.ARRAY, new AbstractDatasetProvider(), IDataset.class));
			}
		}
		return ret;
	}

    /**
     * These are cached and cleared when the model is.
     * @return
     */
	public List<IVariable> getInputVariables() {

		synchronized (cachedUpstreamVariables) {
			
			if (cachedUpstreamVariables.isEmpty()) {
				@SuppressWarnings("rawtypes")
				final List connections = input.connectedPortList();
				for (Object object : connections) {
					final IOPort  port      = (IOPort)object;
					final Object connection =  port.getContainer();
					if (connection instanceof IVariableProvider) {
						final List<IVariable> vars = ((IVariableProvider)connection).getOutputVariables();
						if (vars!=null) cachedUpstreamVariables.addAll(vars);
					}
				}
			}
		}
		return cachedUpstreamVariables;
	}
	
	public boolean isUpstreamVariable(final String name) {
		final List<IVariable> up = getInputVariables();
		for (IVariable iVariable : up) {
			if (iVariable.getVariableName().equals(name)) return true;
		}
		return false;
	}	

	protected abstract String getOperationName();

	@Override
	protected String getExtendedInfo() {
		return getOperationName();
	}
	
	public static void refreshResource(IResource resource) {
		
		if (!(resource instanceof IContainer)) {
			resource = resource.getParent();
		}
		final String resPath = resource.getFullPath().toPortableString();
		try {
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			final Object ob = client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "refresh", new Object[]{resource.getProject().getName(), resPath}, new String[]{String.class.getName(), String.class.getName()});
			if (ob==null || !((Boolean)ob).booleanValue()) {
				logger.error("Cannot refresh resource "+resPath);
			}
		} catch (Exception ne) {
			logger.error("Refreshing project remotely "+resPath);
			logger.trace("Refreshing project remotely "+resPath, ne);
		}

	}

	public IProject getProject() {
		try {
			return ResourceUtils.getProject(this);
		} catch (Exception e) {
			logger.error("Cannot get the project for actor "+getName(), e);
			return null;
		}
	}
	
	
	protected void setDataNames(final DataMessageComponent despatch, final List<DataMessageComponent> inputs) {
		
		if (NAME_MODE.get(2).equals(dataSetNaming.getExpression())) {
			final List<IDataset> sets = MessageUtils.getDatasets(despatch);
			if (sets!=null) for (IDataset iDataset : sets) {
				final String existing = iDataset.getName();
				boolean replacedExisting = despatch.renameList(existing, getName());
				if (replacedExisting) {
					logger.error("Replaced existing name '"+existing+"' with '"+getName()+"' in actor: "+getName());
				}
			}
        
		}
		
		if (NAME_MODE.get(0).equals(dataSetNaming.getExpression())) {
			// If we have one image in the set then we name the set after the
			// file.name
			final List<IDataset> sets = MessageUtils.getDatasets(despatch);
			if (sets!=null && sets.size()==1) {
				final IDataset set = sets.get(0);
				if (set.getShape().length==2) {
				    String fileName = despatch.getScalar("file_name");
				    if (fileName==null&&(inputs!=null&&inputs.size()==1)) {
				    	final DataMessageComponent in = inputs.get(0);
				    	fileName = in.getScalar("file_name");
				    }
				    if (fileName!=null) {
				    	try {
				    		
							final String existing    = set.getName();
							boolean replacedExisting = despatch.renameList(existing, fileName.substring(0,fileName.lastIndexOf('.')));
							if (replacedExisting) {
								logger.error("Replaced existing name '"+existing+"' with '"+fileName.substring(0,fileName.lastIndexOf('.'))+"' in actor: "+getName());
							}
				    	} catch (Exception ignored) {
				    		logger.debug("Could not assign data set name from '"+fileName+"'");
				    	}
				    }
				}
	
			}
			return;
		}
	}
	
	protected void setLastOutput(ManagedMessage o) {
		this.lastOutput = o;
	}
	
	protected ManagedMessage lastOutput;
	
	protected void sendOutputMsg(Port port, ManagedMessage message) throws ProcessingException, IllegalArgumentException {
		if (port==output && hasFinishedPort.numberOfSinks()>0) lastOutput = message;		
		super.sendOutputMsg(port,message);
	}
	
	public NamedObj getObject() {
		return this;
	}

	private String description;
	private Map<Object, String>      descriptions;
	private Map<Object, Requirement> requirements;
	private Map<Object, VariableHandling> variableHandling;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription(Object key) {
		if (descriptions==null) return null;
		return descriptions.get(key);
	}
	/**
	 * 
	 * @param description
	 * @return
	 */
	public Requirement getRequirement(Object key) {
		if (requirements==null || !requirements.containsKey(key)) return Requirement.OPTIONAL;
		return requirements.get(key);
	}
	/**
	 * 
	 * @param key
	 * @param description
	 * @return
	 */
	public VariableHandling getVariableHandling(Object key) {
		if (variableHandling==null || !variableHandling.containsKey(key)) return VariableHandling.NONE;
		return variableHandling.get(key);
	}

	public void setDescription(Object key, Requirement requirement, VariableHandling var, String description) {
		
        if (descriptions==null) descriptions = new IdentityHashMap<Object, String>();
		descriptions.put(key, description);
		
		if (requirements==null) requirements = new IdentityHashMap<Object, Requirement>();
		requirements.put(key, requirement);
		
		if (variableHandling==null) variableHandling = new IdentityHashMap<Object, VariableHandling>();
		variableHandling.put(key, var);
	}

}
