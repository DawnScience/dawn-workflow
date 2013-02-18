/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.dawn;

import java.util.ArrayList;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.function.FunctionParameter;

import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * 
 */
public class FunctionActor extends AbstractDataMessageSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5253638294132148766L;


	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FunctionActor.class);

	
	/** The value produced by this constant source.
	 *  By default, it contains an StringToken with an empty string.  
	 */
	public  FunctionParameter     functionParam;
	public  Parameter        nameParam;
	
	/** Construct a constant source with the given container and name.
	 *  Create the <i>value</i> parameter, initialize its value to
	 *  the default value of an IntToken with value 1.
	 *  @param container The container.
	 *  @param name The name of this actor.
	 *  @exception IllegalActionException If the entity cannot be contained
	 *   by the proposed container.
	 *  @exception NameDuplicationException If the container already has an
	 *   actor with this name.
	 */
	public FunctionActor(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		nameParam = new StringParameter(this, "Name");
		nameParam.setExpression("f(x)");
		nameParam.setDisplayName("Function Name");
		registerConfigurableParameter(nameParam);

		functionParam = new FunctionParameter(this, "Function");
		//functionParam.setFunction(new Fermi(0,0,0,0));// We default it so that something runs.
		registerConfigurableParameter(functionParam);
		
	}
	
	public static FunctionActor createSource(String name, AFunction function) throws NameDuplicationException, IllegalActionException {
		FunctionActor source = new FunctionActor(new CompositeEntity(), name);
		source.functionParam.setFunction(function);
		return source;
	}


	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		super.attributeChanged(attribute);
	}
	
	protected boolean firedOnce = false;
	protected ManagedMessage triggerMsg;
	
	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (firedOnce) return null; // We only send one function from here.
		try {
			DataMessageComponent despatch;
			
			if (triggerMsg!=null) { 
				despatch = MessageUtils.coerceMessage(triggerMsg);
			} else {
				despatch = new DataMessageComponent();
			}
			despatch.addFunction(nameParam.getExpression(), functionParam.getFunction());

			try {
				return MessageUtils.getDataMessage(despatch);
			} catch (Exception e) {
				throw createDataMessageException("Cannot set scalar "+nameParam.getExpression(), e);
			}
		} catch(Exception ne) {
			throw createDataMessageException("Cannot fire "+getName(), ne);
	
		} finally {
			firedOnce = true;
		}
	}	

    /**
	 * @see be.tuple.passerelle.engine.actor.Source#getInfo()
	 */
	protected String getExtendedInfo() {
		return functionParam.getExpression();
	}

	@Override
	public List<IVariable> getOutputVariables() {
		try {
			final String  strName  = ((StringToken) nameParam.getToken()).stringValue();
			final AFunction function      = functionParam.getFunction();
			final List<IVariable> ret = new ArrayList<IVariable>(1);
			ret.add(new Variable(strName, VARIABLE_TYPE.FUNCTION, function, AFunction.class));
			return ret;
		} catch (Exception e) {
			logger.error("Cannot create outputs for "+getName(), e);
		}
		return null;
	}
	
	@Override
	public List<IVariable> getInputVariables() {
		return null;
	}
	
	@Override
	protected boolean mustWaitForTrigger() {
		return trigger.getWidth()>0;
	}
	
	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		this.triggerMsg = triggerMsg;
	}
}
