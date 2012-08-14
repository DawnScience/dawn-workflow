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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;

import org.dawb.passerelle.common.actors.AbstractDataMessageSink;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.PasserelleToken;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.util.ptolemy.IAvailableChoices;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;

/**
 * Attempts to plot data in eclipse or writes a csv file with the data if 
 * that is not possible.
 * 
 * @author gerring
 *
 */
public class MessageSink extends AbstractDataMessageSink {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7807261809740835047L;
	
	private static final Logger logger = LoggerFactory.getLogger(MessageSink.class);
	
	private Parameter messageType,messageParam, messageTitle;

	/**
	 *  NOTE Ports must be public for composites to work.
	 */
    public Port shownMessagePort;

	public MessageSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		messageType = new StringChoiceParameter(this, "Message Type", new IAvailableChoices() {
			
			@Override
			public Map<String, String> getVisibleChoices() {
				final Map<String,String> map = new HashMap<String, String>(3);
				map.put(MessageDialog.ERROR+"",       "ERROR");
				map.put(MessageDialog.WARNING+"",     "WARNING");
				map.put(MessageDialog.INFORMATION+"", "INFORMATION");
				return map;
			}
			
			@Override
			public String[] getChoices() {
				return new String[]{MessageDialog.ERROR+"",MessageDialog.WARNING+"",MessageDialog.INFORMATION+""}; 
			}
		}, SWT.SINGLE);
		messageType.setExpression(MessageDialog.ERROR+"");
		registerConfigurableParameter(messageType);
		
		messageParam = new StringParameter(this, "Message");
		messageParam.setExpression("${message_text}");
		registerConfigurableParameter(messageParam);
		
		messageTitle = new StringParameter(this, "Message Title");
		messageTitle.setExpression("Error Message");
		registerConfigurableParameter(messageTitle);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		passModeParameter.setExpression(EXPRESSION_MODE.get(1));
		passModeParameter.setVisibility(Settable.NONE);
		
		shownMessagePort = PortFactory.getInstance().createOutputPort(this, "shownMessage");

	}

	@Override
	protected void sendCachedData(final List<DataMessageComponent> cache) throws ProcessingException {
		
		try {
			if (cache==null)     return;
			if (cache.isEmpty()) return;
			
			final DataMessageComponent despatch = MessageUtils.mergeAll(cache);
			if (despatch.getScalar()==null || despatch.getScalar().isEmpty()) return;
			
			final String title   = ParameterUtils.getSubstituedValue(messageTitle, cache);
			final String message = ParameterUtils.getSubstituedValue(messageParam, cache);
			final int    type    = Integer.parseInt(messageType.getExpression());
			
			try {
				if (MessageUtils.isErrorMessage(cache)) getManager().stop();

				final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
				if (client!=null) {
					final Object ob = client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "showMessage", new Object[]{title,message,type}, new String[]{String.class.getName(),String.class.getName(),int.class.getName()});
					if (ob==null || !((Boolean)ob).booleanValue()) {
						throw createDataMessageException("Show message '"+getName()+"'!", new Exception());
					}
				}
			} catch (InstanceNotFoundException noService) {
				logger.error(title+">  "+message);
			}
			
			if (shownMessagePort.getWidth()>0) {
			    shownMessagePort.broadcast(new PasserelleToken(MessageUtils.getDataMessage(despatch)));
			}
			
			
		} catch (Exception e) {
			throw createDataMessageException("Cannot show error message '"+getName()+"'", e);
		}
	}

	
	
	public List<IVariable> getOutputVariables() {
		return getInputVariables();
	}

}
