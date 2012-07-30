/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ifdynaport;

import java.util.List;

import org.dawb.passerelle.actors.ifdynaport.ExpressionBean;
import org.dawb.passerelle.actors.ifdynaport.ExpressionContainer;
import org.dawb.passerelle.actors.ifdynaport.ExpressionParameter;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.dynaport.OutputPortSetterBuilder;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.message.ManagedMessage;

public class If extends AbstractDataMessageTransformer {

	private static final Logger logger = LoggerFactory.getLogger(If.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 626830385195273355L;

	public ExpressionParameter expressions;

	public OutputPortSetterBuilder outputPortSetterBuilder;
	
	public If(final CompositeEntity container, final String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		expressions = new ExpressionParameter(this, "Expressions");
		registerConfigurableParameter(expressions);

		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
		
		// The OutputPortSetterBuilder will help us to dynamically create new output ports
		outputPortSetterBuilder = new OutputPortSetterBuilder(this, "output port setter builder");
	}

	/**
	 * The hard part comes at the point where they are dispatched.
	 */
	@Override
	protected DataMessageComponent getTransformedMessage(final List<DataMessageComponent> cache) throws ProcessingException {
        return MessageUtils.mergeAll(cache);
	}
	
	/**
	 * The If actor will send an outgoing message to a specific output port, as defined in the actor's "routing expression".
	 * It is indeed a special kind of Message Router.
	 * <br/>
	 * When the expression can not find a matching port, or when it comes up with a name of a non-existing port,
	 * the message is sent out via the default output port.
	 */
	protected void sendOutputMsg(Port port, ManagedMessage message) throws ProcessingException, IllegalArgumentException {
		String expression, outputPortName;
		if (port == output && MessageUtils.isDataMessage(message) && expressions.getExpression()!=null && !"".equals(expressions.getExpression())) {
			
			// Process if clauses and only dispatch to true ones.
			try {
				final ExpressionContainer cont = (ExpressionContainer)expressions.getBeanFromValue(ExpressionContainer.class);
			    
				for (ExpressionBean expressionBean : cont.getExpressions() ) {
					expression = expressionBean.getExpression();
					if (!expression.equals("false")) {
						if (expression.equals("true") || MessageUtils.isExpressionTrue(expression, message)) {
							outputPortName = expressionBean.getOutputPortName();
							if (logger.isInfoEnabled()) {
								logger.info("Actor '" + this.getName() + "': Expression '" + expression.toString() + 
										"' evaluated to be true, sending message to the '" +  outputPortName + "' port");
							}
							if (((Port)output).getName().equals(outputPortName)) {
								super.sendOutputMsg(((Port)output),message);
							} else {
								for (Object outputPort : this.portList()) {
									if (((Port)outputPort).getName().equals(outputPortName)) {
										super.sendOutputMsg(((Port)outputPort),message);
									}
								}
							}
						}
					}
				}

			} catch (Exception e) {
				throw createDataMessageException("Cannot decode bean from parameter!", e);
			}
		}

	}

	@Override
	protected String getOperationName() {
		return "If";
	}

}
