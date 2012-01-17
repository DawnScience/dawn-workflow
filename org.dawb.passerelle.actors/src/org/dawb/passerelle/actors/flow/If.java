/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.flow;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.PasserelleToken;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.message.ManagedMessage;

public class If extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 626830385195273355L;

	protected ExpressionParameter expressions;
	
	public If(final CompositeEntity container, final String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		expressions = new ExpressionParameter(this, "Expressions");
		registerConfigurableParameter(expressions);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
	}

	/**
	 * The hard part comes at the point where they are dispatched.
	 */
	@Override
	protected DataMessageComponent getTransformedMessage(final List<DataMessageComponent> cache) throws ProcessingException {
        return MessageUtils.mergeAll(cache);
	}
	
	protected void sendOutputMsg(Port port, ManagedMessage message) throws ProcessingException, IllegalArgumentException {
		
		//boolean nothingFired = true;
		
		if (port == output && MessageUtils.isDataMessage(message) && expressions.getExpression()!=null && !"".equals(expressions.getExpression())) {
			
			// Process if clauses and only dispatch to true or null ones.
			try {
				final ExpressionContainer cont = (ExpressionContainer)expressions.getBeanFromValue(ExpressionContainer.class);
			    
				final List channels = port.connectedPortList();
			    for (int channel = 0; channel < channels.size(); channel++) {
					
			    	final NamedObj       dest = ((NamedObj)channels.get(channel)).getContainer();
			    	final ExpressionBean bean = cont.getBean(dest.getName());
			    	if (bean!=null && MessageUtils.isExpressionTrue(bean.getExpression(), message)) {
			    		//nothingFired = false;
				    	Token token = new PasserelleToken(message);
				    	port.send(channel, token);
			    	}
				}
			    
			    
			} catch (Exception e) {
				throw createDataMessageException("Cannot decode bean from parameter!", e);
			}
		}

//		if (nothingFired) {
//		    super.sendOutputMsg(port,message);
//		}
	}

	@Override
	protected String getOperationName() {
		return "If";
	}

}
