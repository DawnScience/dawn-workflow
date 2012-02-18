/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.data;

import java.util.List;

import org.dawb.passerelle.actors.flow.ExpressionBean;
import org.dawb.passerelle.actors.flow.ExpressionContainer;
import org.dawb.passerelle.actors.flow.ExpressionParameter;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Actor which simply evaluates expressions and passes them on.
 * @author gerring
 *
 */
public class Expression extends AbstractDataMessageTransformer {

	protected ExpressionParameter expressions;

	public Expression(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		expressions = new ExpressionParameter(this, "Expressions");
		expressions.setNameParameter("Expression Result");
		expressions.setAutomaticExpressionCreation(false);
		registerConfigurableParameter(expressions);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5727187853952573397L;

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		final DataMessageComponent ret = new DataMessageComponent();
		ret.setMeta(MessageUtils.getMeta(cache));
		ret.addScalar(MessageUtils.getScalar(cache));

		final ExpressionContainer cont = (ExpressionContainer)expressions.getBeanFromValue(ExpressionContainer.class);

		try {
			DataMessageComponent input = MessageUtils.mergeAll(cache);

			final List<ExpressionBean> beans = cont.getExpressions();
			for (ExpressionBean eb : beans) {
				ret.putScalar(eb.getOutputPortName(), String.valueOf(MessageUtils.evaluateExpression(eb.getExpression(), input)));
			}
			
			return ret;
			
		} catch (Exception e) {
			throw createDataMessageException("Cannot decode bean from parameter!", e);
		}
	}

	@Override
	protected String getOperationName() {
		return "Expression Evaluation";
	}

}
