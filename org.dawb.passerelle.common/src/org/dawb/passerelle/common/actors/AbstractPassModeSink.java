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
import java.util.List;

import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.Actor;
import ptolemy.actor.IOPort;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ChangeListener;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.Sink;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.MessageHelper;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;

public abstract class AbstractPassModeSink extends Sink implements IVariableProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPassModeSink.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1903160956377231213L;

	protected static List<String> EXPRESSION_MODE;
	static {
		EXPRESSION_MODE = new ArrayList<String>(3);
		EXPRESSION_MODE.add("Evaluate on every data input");
		EXPRESSION_MODE.add("Evaluate after all data received");
		EXPRESSION_MODE.add("Evaluate on finished");
		EXPRESSION_MODE.add("Evaluate on first message then never again");
	}
	
	protected static List<String> MEMORY_MODE;
	static {
		MEMORY_MODE = new ArrayList<String>(3);
		MEMORY_MODE.add("Create copy of data leaving original data intact.");
		MEMORY_MODE.add("Operate on data directly to save memory.");
	}
	
	protected final StringChoiceParameter passModeParameter;
	protected String                      passMode;

	protected final StringChoiceParameter memoryManagementParam;
	protected String                      memoryMode;

	
	/**
	 * Could be protected but no need at the moment.
	 */
	private RecordingPortHandler recInputHandler;

	/**
	 * Cached variables
	 */
	protected ArrayList<IVariable> cachedUpstreamVariables;

    public AbstractPassModeSink(final CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, ModelUtils.findUniqueActorName(container, name));
		
		passModeParameter = new StringChoiceParameter(this, "Expression Mode", getExpressionModes(), SWT.SINGLE);
		passModeParameter.setExpression(EXPRESSION_MODE.get(0));
		registerConfigurableParameter(passModeParameter);
		passMode = EXPRESSION_MODE.get(0);
		
		
		memoryManagementParam = new StringChoiceParameter(this, "Memory Mode", getMemoryModes(), SWT.SINGLE);
		memoryManagementParam.setExpression(MEMORY_MODE.get(0));
		registerConfigurableParameter(memoryManagementParam);
		memoryMode = MEMORY_MODE.get(0);
		
		this.cachedUpstreamVariables = new ArrayList<IVariable>(7);

		// Any change upsteam means they are invalid.
		container.addChangeListener(new ChangeListener() {		
			@Override
			public void changeFailed(ChangeRequest change, Exception exception) { }
			@Override
			public void changeExecuted(ChangeRequest change) {
				cachedUpstreamVariables.clear();
				if (!container.deepContains(AbstractPassModeSink.this)) {
					container.removeChangeListener(this);
				}
			}
		});

	}
	
	/*
	 *  (non-Javadoc)
	 * @see be.isencia.passerelle.actor.Actor#doInitialize()
	 */
	protected void doInitialize() throws InitializationException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo());
			
		if(input.getWidth()>0) {
			recInputHandler = new RecordingPortHandler(input, true);
			recInputHandler.start();
		} else {
			requestFinish();
		}
		
		if(logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit ");

	}
	protected boolean doPreFire() throws ProcessingException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPreFire() - entry");
		
		Token token = recInputHandler.getToken();
		if (token != null) {
			try {
				message = MessageHelper.getMessageFromToken(token);
			} catch (PasserelleException e) {
						throw new ProcessingException("Error handling token", token, e);
			}
		} else {
			message = null;
		}

		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPreFire() - exit");
		return true;
	}


	/**
	 * Override to provide different options for processing the
	 * ports.
	 * 
	 * @return
	 */
	protected List<String> getExpressionModes() {
		return EXPRESSION_MODE;
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
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == passModeParameter) {
			passMode = passModeParameter.getExpression();
		}
		super.attributeChanged(attribute);
	}

	protected String getPassMode() {
		return passMode;
	}

	protected void setPassMode(String passMode) {
		this.passMode = passMode;
	}
	
	protected boolean isFireInLoop() {
		return EXPRESSION_MODE.get(0).equals(passMode);
	}
	
	protected boolean isFireEndLoop() {
		return EXPRESSION_MODE.get(1).equals(passMode);
	}
	
	protected boolean isFireOnFinished() {
		return EXPRESSION_MODE.get(2).equals(passMode);
	}
	
	protected boolean isFireOnce() {
		return EXPRESSION_MODE.get(3).equals(passMode);
	}
	
	protected boolean isCreateClone() {
		return MEMORY_MODE.get(0).equals(memoryMode);
	}

    /**
     * These are cached and cleared when the model is.
     * @return
     */
	public List<IVariable> getInputVariables() {

		synchronized (cachedUpstreamVariables) {
			
			if (cachedUpstreamVariables.isEmpty()) {
				@SuppressWarnings("rawtypes")
				final List connections = this.input.connectedPortList();
				for (Object object : connections) {
					final IOPort port      = (IOPort)object;
					final Actor connection = (Actor)port.getContainer();
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
	
	public List<IVariable> getOutputVariables() {
		return null;
	}

	
    /**
     * Returns true when each input has fired received one message and 
     * the queue has dealt with it.
     * 
     * If true is returned then the count of which port has been received is
     * reset. True is returned once and then reset, after all input wires have
     * fired again, it will be true again.
     * 
     * @return
     */
	protected boolean isInputRoundComplete() {
		return recInputHandler.isInputComplete();
	}

}
