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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.services.ServiceManager;
import org.dawb.common.services.expressions.IExpressionEngine;
import org.dawb.common.services.expressions.IExpressionService;
import org.dawb.passerelle.actors.flow.ExpressionBean;
import org.dawb.passerelle.actors.flow.ExpressionContainer;
import org.dawb.passerelle.actors.flow.ExpressionParameter;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Actor which simply evaluates expressions and passes them on.
 * @author gerring
 *
 */
public class Expression extends AbstractDataMessageTransformer {

	protected static final Logger logger = LoggerFactory.getLogger(Expression.class);
	
	protected ExpressionParameter expressions;
	private IExpressionService  expressionService;

	public Expression(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		expressions = new ExpressionParameter(this, "Expressions");
		expressions.setNameParameter("Expression Result");
		expressions.setAutomaticExpressionCreation(false);
		registerConfigurableParameter(expressions);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
		
		try {
			this.expressionService = (IExpressionService)ServiceManager.getService(IExpressionService.class);
		} catch (Exception e) {
			logger.error("Cannot get expression service", e);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5727187853952573397L;

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		final DataMessageComponent ret = MessageUtils.copy(cache);
		
		// Get the Expression Engine
		final IExpressionEngine engine = expressionService.getExpressionEngine();
		engine.getFunctions().put("math", Math.class);
		final ExpressionContainer cont = (ExpressionContainer)expressions.getBeanFromValue(ExpressionContainer.class);

		try {
			
			final Map<String, Object> values = new HashMap<String, Object>(9);
			
			if (ret.getList()!=null) values.putAll(ret.getList());
			for (String key : ret.getScalar().keySet()) {
				String value = ret.getScalar().get(key);
				try {
					double doubleValue = Double.parseDouble(value);
					values.put(key, doubleValue);
				} catch (Exception e) {
					values.put(key, value);
				}
			}
			
			engine.setLoadedVariables(values);
			
			final List<ExpressionBean> beans = cont.getExpressions();
			for (ExpressionBean eb : beans) {
				try {
					engine.createExpression(eb.getExpression());
					Object result = engine.evaluate();
					if (result instanceof IDataset) {
						ret.addList(eb.getVariableName(), (IDataset) result);
					} else {
						ret.putScalar(eb.getVariableName(), result.toString());
					}
					engine.addLoadedVariable(eb.getVariableName(), result);
				} catch (Exception e) {
					// Record in the log that this expression has failed, but carry on, as this may not matter
					logger.warn("Failed to process the expression {}", eb.getExpression(), e);
				}
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
