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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.dawb.common.util.SubstituteUtils;
import org.dawb.passerelle.actors.ui.config.FieldBean;
import org.dawb.passerelle.actors.ui.config.FieldContainer;
import org.dawb.passerelle.actors.ui.config.FieldParameter;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.workbench.jmx.ActorSelectedBean;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.dawb.workbench.jmx.UserInputBean;
import org.eclipse.swt.SWT;
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
public class UserModifyTransformer extends AbstractDataMessageTransformer {

	private static final Logger logger = LoggerFactory.getLogger(UserModifyTransformer.class);

	protected static final String[] INPUT_CHOICES;
	static {
		INPUT_CHOICES = new String[]{"Edit with dialog (non-blocking)", "Edit with editor part (non-blocking)"};
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -8123070610686014566L;

	private FieldParameter  fieldParam;
	private StringParameter inputTypeParam, automaticModeParam;
	private Parameter       silent;
	
	public UserModifyTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
		
		fieldParam = new FieldParameter(this, "User Fields");
		registerConfigurableParameter(fieldParam);

		inputTypeParam = new StringParameter(this,"User Input Type") {

			public String[] getChoices() {
				return INPUT_CHOICES;
			}
		};
		
		silent = new Parameter(this, "Silent");
		silent.setToken(new BooleanToken(false));
		registerConfigurableParameter(silent);

		automaticModeParam = new StringParameter(this, "Automatic Mode Variable");
		automaticModeParam.setExpression("automatic_mode");
		registerConfigurableParameter(automaticModeParam);

		
		inputTypeParam.setExpression(INPUT_CHOICES[0]);
		registerConfigurableParameter(inputTypeParam);

	}

	protected boolean doPreFire() throws ProcessingException {
        return super.doPreFire();
	}
	
	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		try {
            if (getManager()!=null &&
            	(getManager().isExitingAfterWrapup() || getManager().getState()==Manager.EXITING || getManager().getState()==Manager.WRAPPING_UP)) {
            	return null;
            }
            
			final Map<String,String>    scalar = MessageUtils.getScalar(cache);
			
			// Check if automatic mode
			String automaticModeExpression = automaticModeParam.getExpression();
			if (scalar.containsKey(automaticModeExpression)) {
				final String automaticMode = scalar.get(automaticModeExpression);
				logInfo("Automatic mode : "+automaticMode);
				if (automaticMode.equals("true")) {
					final DataMessageComponent  ret    = new DataMessageComponent();
					ret.setMeta(MessageUtils.getMeta(cache));
					ret.addScalar(scalar);
					return ret;				
				}
			}	else {
				scalar.put(automaticModeParam.getExpression(), "false");
			}
				
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			if (client == null ) {
				final DataMessageComponent  ret    = new DataMessageComponent();
				ret.setMeta(MessageUtils.getMeta(cache));
				ret.addScalar(scalar);
				return ret;
			}
			
			boolean isDialog = INPUT_CHOICES[0].equals(inputTypeParam.getExpression());
			
			try {
				
				ActorUtils.setLocked(true);
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{new ActorSelectedBean(getModelPath(), getName(), true, SWT.COLOR_RED)}, new String[]{ActorSelectedBean.class.getName()});
			
				final UserInputBean bean = new UserInputBean();
				bean.setActorName(getName());
				bean.setPartName("Review");
				bean.setDialog(isDialog);
				bean.setConfigurationXML(fieldParam.getXML());
				bean.setScalar(scalar);				
				bean.setSilent(((BooleanToken)silent.getToken()).booleanValue());
				
				final Object       ob    = client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "createUserInput", new Object[]{bean}, new String[]{UserInputBean.class.getName()});
				Map<String,String> trans = (Map<String,String>)ob;
				if (trans==null) {
					trans = new HashMap<String,String>(7);
					final FieldContainer fc = (FieldContainer)fieldParam.getBeanFromValue(FieldContainer.class);
					for (FieldBean fb : fc.getFields()) {
						if (fb.getDefaultValue()!=null) trans.put(fb.getVariableName(), fb.getDefaultValue().toString());
					}
				} else if (trans.isEmpty()) {
					requestFinish();
					return null;
				}
				
				final DataMessageComponent  ret    = new DataMessageComponent();
				ret.setMeta(MessageUtils.getMeta(cache));
				ret.addScalar(MessageUtils.getScalar(cache));
				ret.addScalar(trans);
				
				return ret;
			} finally {
				ActorUtils.setLocked(false);
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{new ActorSelectedBean(getModelPath(), getName(), false, -1)}, new String[]{ActorSelectedBean.class.getName()});
			}
			
		} catch (Exception e) {
			throw createDataMessageException("Cannot allow user to modify message '"+getName()+"'", e);
		}
	}

	@Override
	protected String getOperationName() {
		return "User modify scalar values";
	}

	
	
	@Override
	public List<IVariable> getOutputVariables() {
		
		try {
		    final List<IVariable>    ret           = super.getOutputVariables();
			final FieldContainer     fc            = (FieldContainer)fieldParam.getBeanFromValue(FieldContainer.class);
			if (fc==null || fc.isEmpty()) return ret;
			
			for (FieldBean fb : fc.getFields()) {
			    ret.add(new Variable(fb.getVariableName(), VARIABLE_TYPE.SCALAR, fb.getDefaultValue(), String.class));
			}
			
			return ret;
			
		} catch (Exception e) {
			logger.error("Cannot read variables", e);
			return null;
		}

	}

}
