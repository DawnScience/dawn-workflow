/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.dawb.common.services.IClassLoaderService;
import org.dawb.common.util.list.ListUtils;
import org.dawb.passerelle.common.Activator;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.ActorSelectedBean;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.BatchToolFactory;
import org.dawnsci.passerelle.tools.IBatchTool;
import org.dawnsci.plotting.api.tool.ToolPageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.Manager;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Allows user to modify scalar values in the message and
 * blocks the workflow until they have.
 * 
 * @author gerring
 *
 */
public class UserPlotTransformer extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7766071772167702705L;

	private static final Logger logger = LoggerFactory.getLogger(UserPlotTransformer.class);

	protected static final String[] INPUT_CHOICES;
	static {
		INPUT_CHOICES = new String[]{"Edit with dialog (non-blocking)", "Edit with editor part (non-blocking)"};
	}
	
	/**
	 * Requires all the RCP stuff to be there but it is in the context where this
	 * actor is used.
	 */
	protected static String[] TOOL_CHOICES;
	static {
		List<String> ids;
		try {
			ids = ToolPageFactory.getToolPageIds();
			TOOL_CHOICES     = ids.toArray(new String[ids.size()]);
		} catch (Exception e) {
			TOOL_CHOICES     = new String[]{"No tools found registered"};
		}
	}

	private StringParameter inputTypeParam, toolId, description, axisNames, dataNames;
	private Parameter       silent, autoApplyDefault;
	
	public UserPlotTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
		
		inputTypeParam = new StringParameter(this,"User Input Type") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String[] getChoices() {
				return INPUT_CHOICES;
			}
		};
		inputTypeParam.setExpression(INPUT_CHOICES[0]);
		registerConfigurableParameter(inputTypeParam);
		
		toolId = new StringParameter(this,"Tool id") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public String[] getChoices() {
				return TOOL_CHOICES;
			}
		};
		registerConfigurableParameter(toolId);
		
		description = new StringParameter(this, "Description");
		registerConfigurableParameter(description);
		
		axisNames = new StringParameter(this, "Axis Names");
		registerConfigurableParameter(axisNames);
		
		dataNames = new StringParameter(this, "Data Names");
		registerConfigurableParameter(dataNames);

		silent = new Parameter(this, "Silent");
		silent.setToken(new BooleanToken(false));
		registerConfigurableParameter(silent);

		autoApplyDefault = new Parameter(this, "Automatic default value");
		autoApplyDefault.setToken(new BooleanToken(true));
		registerConfigurableParameter(autoApplyDefault);

	}

	protected boolean doPreFire() throws ProcessingException {
        return super.doPreFire();
	}
	
	/**
	 * These two fields are used if they have run the tool once and
	 * would like to run it from here onwards in batch.
	 */
	private boolean      isAutomaticallyProcess=false;
	private Serializable toolSetup=null;
	
	@SuppressWarnings("unchecked")
	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		try {
            if (getManager()!=null &&
            	(getManager().isExitingAfterWrapup() || getManager().getState()==Manager.EXITING || getManager().getState()==Manager.WRAPPING_UP)) {
            	return null;
            }
            
			final Map<String,String>    scalar = MessageUtils.getScalar(cache);
				
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			if (client == null ) {
				final DataMessageComponent  ret    = new DataMessageComponent();
				ret.setMeta(MessageUtils.getMeta(cache));
				ret.addScalar(scalar);
				return ret;
			}	
			
			boolean isDialog = INPUT_CHOICES[0].equals(inputTypeParam.getExpression());
			
			try {
				
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{new ActorSelectedBean(getModelPath(), getName(), true, 3/**SWT.COLOUR_RED**/)}, new String[]{ActorSelectedBean.class.getName()});
			
				DataMessageComponent input = MessageUtils.mergeAll(cache);
				final UserPlotBean bean = new UserPlotBean();
				bean.setActorName(getName());
				bean.setPartName("Review Plot");
				bean.setDialog(isDialog);
				bean.setScalar(scalar);
				bean.setData(getList(input));
				bean.setRois(input.getRois());
				bean.setFunctions(input.getFunctions());
				bean.setToolId(toolId.getExpression());
				bean.setSilent(((BooleanToken)silent.getToken()).booleanValue());
				bean.setAxesNames(getAxesNames());
				bean.setDataNames(getDataNames());
				bean.setFunctions(input.getFunctions());
				bean.setAutomaticallyApply(((BooleanToken)autoApplyDefault.getToken()).booleanValue());
				bean.setToolData(toolSetup);
				
				if (description.getExpression()!=null && !"".equals(description.getExpression())) {
					bean.setDescription(description.getExpression());
				}
				
				IClassLoaderService service = (IClassLoaderService)Activator.getService(IClassLoaderService.class);
				try {
					if (service!=null) service.setDataAnalysisClassLoaderActive(true);
					
					UserPlotBean uRet=null;
					if (isAutomaticallyProcess) {
						final IBatchTool batchTool = BatchToolFactory.getBatchTool(toolId.getExpression());
						if (batchTool==null) throw new Exception("Batch tool for plot tool "+toolId.getExpression()+" does not exist!");
						uRet = batchTool.process(bean, this);
					} else {
						uRet = (UserPlotBean)client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "createPlotInput", new Object[]{bean}, new String[]{UserPlotBean.class.getName()});
					}
					if (uRet==null || uRet.isEmpty()) {
						requestFinish();
						return null;
					} 
					
					isAutomaticallyProcess = uRet.isAutomaticallyApply();
					
					// @TODO Improve the user plot bean mechanism in dedicated tools (ImageProcessingTool)
					toolSetup = uRet.getToolData();
					if ((toolSetup != null) && (toolSetup instanceof UserPlotBean)) {
						UserPlotBean plotBean = (UserPlotBean) toolSetup;
						input.addScalar(plotBean.getScalar());				
						input.addList(plotBean.getData());
						input.addRois(plotBean.getRois());
						input.addFunctions(plotBean.getFunctions());
					}
	
					input.setMeta(MessageUtils.getMeta(cache));
					input.addScalar(uRet.getScalar());				
					input.addList(uRet.getData());
					input.addRois(uRet.getRois());
					input.addFunctions(uRet.getFunctions());
					
					return input;
					
				} finally {
					if (service!=null) service.setDataAnalysisClassLoaderActive(false);
				}
				
			} finally {
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{new ActorSelectedBean(getModelPath(), getName(), false, -1)}, new String[]{ActorSelectedBean.class.getName()});
			}
			
		} catch (Exception e) {
			throw createDataMessageException("Cannot allow user to modify message '"+getName()+"'", e);
		}
	}

	private List<String> getAxesNames() {
		return ListUtils.getList(axisNames.getExpression());
	}
	private List<String> getDataNames() {
		return ListUtils.getList(dataNames.getExpression());
	}

	private Map<String, Serializable> getList(DataMessageComponent input) {
		
		final Map<String,Serializable> data = new HashMap<String, Serializable>(input.getList());
		final List<String> retainedNames = getDataNames();
		if (retainedNames!=null && !retainedNames.isEmpty()) {
			if (getAxesNames()!=null) retainedNames.addAll(getAxesNames());
			data.keySet().retainAll(retainedNames);
		}
		return data;
	}

	@Override
	protected String getOperationName() {
		return "User plot";
	}

	
	
	@Override
	public List<IVariable> getOutputVariables() {
		
		try {
		    final List<IVariable>    ret           = super.getOutputVariables();
		    // TODO 
		    
			return ret;
			
		} catch (Exception e) {
			logger.error("Cannot read variables", e);
			return null;
		}

	}

}
