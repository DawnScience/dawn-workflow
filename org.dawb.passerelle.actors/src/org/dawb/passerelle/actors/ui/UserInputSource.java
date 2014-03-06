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

import org.dawb.passerelle.actors.ui.config.FieldBean;
import org.dawb.passerelle.actors.ui.config.FieldContainer;
import org.dawb.passerelle.actors.ui.config.FieldParameter;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawb.passerelle.common.utils.SubstituteUtils;
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

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;

public class UserInputSource extends AbstractDataMessageSource {

	
	protected static final String[] INPUT_CHOICES;
	static {
		INPUT_CHOICES = new String[]{"Edit with dialog (non-blocking)", "Edit with editor part (non-blocking)"};
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1963665538170982944L;
	private static final Logger logger = LoggerFactory.getLogger(UserInputSource.class);
	
	private FieldParameter   fieldParam;
	private StringParameter  inputTypeParam;
	private Parameter        silent;
	
	private boolean haveSendMessage = false;
	private ManagedMessage triggerMsg;

	public UserInputSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
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
		
		inputTypeParam.setExpression(INPUT_CHOICES[0]);
		registerConfigurableParameter(inputTypeParam);
	}
	
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (trigger.getWidth()<=0 && haveSendMessage) return null;
	
		try {
            if (getManager()!=null &&
            	(getManager().isExitingAfterWrapup() || getManager().getState()==Manager.EXITING || getManager().getState()==Manager.WRAPPING_UP)) {
            	return null;
            }
            
			final MBeanServerConnection client     = ActorUtils.getWorkbenchConnection();			
			Map<String,String> scalarValues = null;
			if (triggerMsg!=null) {
				try {
					final DataMessageComponent c = MessageUtils.coerceMessage(triggerMsg);
					scalarValues = c.getScalar();
				} catch (Exception ignored) {
					logger.info("Trigger for "+getName()+" is not DataMessageComponent, no data added.");
				}
			}
			
			if (client==null) {
				final DataMessageComponent  ret    = new DataMessageComponent();
				ret.addScalar(scalarValues);
				
				return MessageUtils.getDataMessage(ret, null);
			}

			boolean isDialog = INPUT_CHOICES[0].equals(inputTypeParam.getExpression());
			
			try {
				
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{new ActorSelectedBean(getModelPath(), getName(), true, SWT.COLOR_RED)}, new String[]{ActorSelectedBean.class.getName()});
				
				final UserInputBean bean = new UserInputBean();
				bean.setActorName(getName());
				bean.setPartName("Set Values");
				bean.setDialog(isDialog);
				final String xml = ParameterUtils.substitute(fieldParam.getXML(), this);
				bean.setConfigurationXML(xml);
				bean.setScalar(scalarValues);				
				bean.setSilent(((BooleanToken)silent.getToken()).booleanValue());
				
				final Object       ob    = client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "createUserInput", new Object[]{bean}, new String[]{UserInputBean.class.getName()});
				Map<String,String> trans = (Map<String,String>)ob;
				if (trans==null) {
					trans = new HashMap<String,String>(7);
					final FieldContainer fc = (FieldContainer)fieldParam.getBeanFromValue(FieldContainer.class);
					for (FieldBean fb : fc.getFields()) {
						if (fb.getDefaultValue()!=null) trans.put(fb.getVariableName(), ParameterUtils.substitute(fb.getDefaultValue().toString(), this));
					}
				} else if (trans.isEmpty()) {
					requestFinish();
					return null;
				}
				
				// We always send the variables from the trigger on too, although it
				// may not have been presented to the user.
				final Map<String,String>sentScalars  = new HashMap<String,String>(7);
				if (scalarValues!=null) sentScalars.putAll(scalarValues);
				if (trans!=null)        sentScalars.putAll(trans);
	
				final DataMessageComponent  ret    = new DataMessageComponent();
				ret.addScalar(sentScalars);
				
				return MessageUtils.getDataMessage(ret, null);
			
			} finally {
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{new ActorSelectedBean(getModelPath(), getName(), false, -1)}, new String[]{ActorSelectedBean.class.getName()});
			}
			
		} catch (Exception e) {
			throw createDataMessageException("Cannot allow user to modify message '"+getName()+"'", e);
		} finally {
			haveSendMessage = true;
		}
	}

	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		this.triggerMsg = triggerMsg;
	}

	@Override
	protected boolean mustWaitForTrigger() {
		return trigger.getWidth()>=1;
	}

	@Override
	protected String getExtendedInfo() {
		return "A source of information from a user input form";
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
