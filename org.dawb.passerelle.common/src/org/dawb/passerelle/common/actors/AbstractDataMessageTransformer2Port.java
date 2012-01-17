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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
 * A Transformer which caches all messages coming into the doFire() and then
 * forces the implemented to define a method to generate the output message
 * from the cache. The cache is ordered although the order in which the messages
 * are recieved in the first place is not guaranteed.
 * 
 * The two ports can be used for non-symmetric operations like substract, divide.
 * 
 * @author gerring
 *
 */
public abstract class AbstractDataMessageTransformer2Port extends AbstractPassModeTransformer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6609512817357858738L;
	
	
	protected final List<DataMessageComponent> port1Cache;
	protected final List<DataMessageComponent> port2Cache;
	
	/**
	 *  NOTE Ports must be public for composites to work.
	 */
    public  Port           inputPort2;
	private PortHandler    inputHandler2;

	protected static final List<String> TWO_PORT_EXPRESSION_MODES;
	static {
		TWO_PORT_EXPRESSION_MODES = new ArrayList<String>(7);
		TWO_PORT_EXPRESSION_MODES.addAll(EXPRESSION_MODE);
		TWO_PORT_EXPRESSION_MODES.add("Evaluate when input to a and cache b");
	}
	
	protected static final Logger logger = LoggerFactory.getLogger(AbstractDataMessageTransformer2Port.class);

	public AbstractDataMessageTransformer2Port(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		port1Cache = new ArrayList<DataMessageComponent>(7);
		port2Cache = new ArrayList<DataMessageComponent>(7);
		input.setName("a");
		inputPort2 = PortFactory.getInstance().createInputPort(this, "b", ManagedMessage.class);
		
		// Default is to process a and sum b
		passModeParameter.setExpression( TWO_PORT_EXPRESSION_MODES.get(2));
		passMode = TWO_PORT_EXPRESSION_MODES.get(2);
	}
	
	public void doPreInitialize() {
		port1Cache.clear();
		port2Cache.clear();
	}
	/**
	 * Override to provide different options for processing the
	 * ports.
	 * 
	 * @return
	 */
	protected List<String> getExpressionModes() {
		return TWO_PORT_EXPRESSION_MODES;
	}
	
	protected boolean isFireInLoop() {
		if (port1Cache==null||port1Cache.size()<getMinimumCacheSize()) return false;
		if (port2Cache==null||port2Cache.size()<getMinimumCacheSize()) return false;
		return super.isFireInLoop();
	}
	
	protected int getMinimumCacheSize() {
		return 1;
	}

	protected abstract DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException;
	
	/*
	 *  (non-Javadoc)
	 * @see be.isencia.passerelle.actor.Actor#doInitialize()
	 */
	protected void doInitialize() throws InitializationException {
		
		this.inputHandler2 = new PortHandler(inputPort2);

		if (inputPort2.getWidth()>0) {
			inputHandler2.start();
		}
		super.doInitialize();
			
	}

	protected boolean doPreFire() throws ProcessingException {
		
		if (passMode.equals(TWO_PORT_EXPRESSION_MODES.get(2))) {
						
			Token token = null;
			try {

				while((token=inputHandler2.getToken())!=null) {
					ManagedMessage message2  = MessageHelper.getMessageFromToken(token);
					DataMessageComponent msg = MessageUtils.coerceMessage(message2);
					port2Cache.add(msg);
				}
			} catch (Exception e) {
			    throw new ProcessingException("Error handling token", token, e);
			}
		} else {
			Token token = inputHandler2.getToken();
			if (token != null) {
				try {
					ManagedMessage message2  = MessageHelper.getMessageFromToken(token);
					DataMessageComponent msg = MessageUtils.coerceMessage(message2);
					port2Cache.add(msg);

				} catch (Exception e) {
				    throw new ProcessingException("Error handling token", token, e);
				}
			}
		}
		return super.doPreFire();
	}
	
	@Override
	protected void doFire(ManagedMessage message) throws ProcessingException {
		
		try {
			if (message!=null) {
				DataMessageComponent msg = MessageUtils.coerceMessage(message);
				port1Cache.add(msg);
			}
			
			if (isFireInLoop() || isLoopPort1SumPort2(true)) {
				final DataMessageComponent despatch = getDespatch();
				if (despatch==null) return;
				sendOutputMsg(output, MessageUtils.getDataMessage(despatch));
			}
			
		} catch (ProcessingException pe) {
			throw pe;
		} catch (Exception ne) {
			throw createDataMessageException("Cannot add data from '"+message+"'", ne);
		}
	}
	
	private DataMessageComponent getDespatch() throws ProcessingException {
		
		try {
			ActorUtils.setActorExecuting(this, true);
			
			if (MessageUtils.isScalarOnly(port1Cache)) {
				throw createDataMessageException("Cannot send messages with scalar data only to port 'a' of '"+getName()+"'", null);
			}
			
			final DataMessageComponent despatch = getTransformedMessage(port1Cache, MessageUtils.mergeScalar(port2Cache));
			if (despatch!=null) setDataNames(despatch, port1Cache);

			port1Cache.clear();
			if (!isLoopPort1SumPort2(false)) {
				port2Cache.clear();
			}
			if (despatch==null) return null;
			
			despatch.putScalar("operation.time."+getName(), DateFormat.getDateTimeInstance().format(new Date()));
			despatch.putScalar("operation.type."+getName(), getOperationName());

			return despatch;
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}
	


	protected boolean doPostFire() throws ProcessingException {
		
		final boolean isFinished   = isFinishRequested();
		final boolean isInputRound = isInputRoundComplete();
		
		if (isInputRound && isFireEndLoop()) {
			try {
				final DataMessageComponent despatch = getDespatch();
				if (despatch==null) return !isFinished; 
				sendOutputMsg(output, MessageUtils.getDataMessage(despatch));
			} catch (ProcessingException pe) {
				throw pe;
			} catch (Exception ne) {
				logger.error("Cannot transform data", ne);
			}
		}
		
		if (isFinished) return false;
		return true; // Wait for more
	}
	
	protected void doWrapUp() throws TerminationException {
		super.doWrapUp();
		if (isFinishRequested()) {
			port1Cache.clear();
			port2Cache.clear();
		}
	}

	protected boolean isLoopPort1SumPort2(boolean checkCacheState) {
		
		if (checkCacheState) {
			if (port1Cache==null||port1Cache.size()<getMinimumCacheSize()) return false;
			if (passMode.equals(TWO_PORT_EXPRESSION_MODES.get(2))) {
				final int size = inputPort2.getWidth();
				if (port2Cache==null||port2Cache.size()<size) return false;
			} else {
			    if (port2Cache==null||port2Cache.size()<getMinimumCacheSize()) return false;
			}
		}
		return passMode.equals(TWO_PORT_EXPRESSION_MODES.get(2));
	}

	protected void setUpstreamValues(final DataMessageComponent ret,
									final List<DataMessageComponent> port1Cache,
									final List<DataMessageComponent> port2Cache) {
		
		ret.setMeta(MessageUtils.getMeta(port1Cache));
		ret.addScalar(MessageUtils.getScalar(port1Cache));
		ret.addScalar(MessageUtils.getScalar(port2Cache), false);
	}
	
	protected DataMessageException createDataMessageException(String msg, Throwable e) throws DataMessageException {
		final Collection<DataMessageComponent> inputs = new ArrayList<DataMessageComponent>(7);
		inputs.addAll(port2Cache);
		inputs.addAll(port1Cache);
		return new DataMessageException(msg, this, inputs, e);
	}

}
