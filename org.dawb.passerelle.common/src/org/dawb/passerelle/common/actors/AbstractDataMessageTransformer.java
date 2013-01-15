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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.dawb.workbench.jmx.UserDebugBean.DebugType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageFactory;

/**
 * A Transformer which caches all messages coming into the doFire() and then
 * forces the implemented to define a method to generate the output message
 * from the cache. The cache is ordered although the order in which the messages
 * are recieved in the first place is not guaranteed.
 * 
 * So subclasses can implement more ports and then be sure of order.
 * 
 * @author gerring
 *
 */
public abstract class AbstractDataMessageTransformer extends AbstractPassModeTransformer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8697753304302318301L;
	
	protected final List<DataMessageComponent> cache;
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractDataMessageTransformer.class);

	public AbstractDataMessageTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		cache = new ArrayList<DataMessageComponent>(7);
	}
	
	public void doPreInitialize() throws InitializationException{
		cache.clear();
	}
	
	protected boolean alreadyFiredOneBlankMessage = false;
	
	protected boolean doPreFire() throws ProcessingException {
		
        final boolean ret = super.doPreFire();
        
        // We fire once a blank message.
        if (getMinimumCacheSize()<1 && message==null && input.getWidth()<1 && !alreadyFiredOneBlankMessage) {
        	alreadyFiredOneBlankMessage = true;
        	message = MessageFactory.getInstance().createMessage();
        }
        return ret;
	}

	protected abstract DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException;
	
	@Override
	protected void doFire(ManagedMessage message) throws ProcessingException {
		
		try {
			if (message!=null) {
				DataMessageComponent msg = MessageUtils.coerceMessage(message);
				cache.add(msg);
			}
			
			if (isFireInLoop()) {
				
				final DataMessageComponent despatch = getDespatch();
				if (despatch==null) return;
				
		        sendOutputMsg(output, MessageUtils.getDataMessage(despatch));
				cache.clear();
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
			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(cache), DebugType.BEFORE_ACTOR);
				ActorUtils.debug(bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}

			final DataMessageComponent despatch = getTransformedMessage(cache);
			if (despatch!=null) setDataNames(despatch, cache);
			if (despatch==null) return null;
			
			despatch.putScalar("operation.time."+getName(), DateFormat.getDateTimeInstance().format(new Date()));
			despatch.putScalar("operation.type."+getName(), getOperationName());
			return despatch;
			
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}

	protected boolean isFireInLoop() {
		if (cache==null||cache.size()<getMinimumCacheSize()) return false;
		return super.isFireInLoop();
	}
	
	protected int getMinimumCacheSize() {
		return 1;
	}

	protected boolean doPostFire() throws ProcessingException {
		
		final boolean isFinished   = isFinishRequested();
		final boolean isInputRound = isInputRoundComplete();

		// TODO Check also if all inputs to input have fired once, not only if system has finished.
		if (isInputRound && isFireEndLoop() && cache.size()>=getMinimumCacheSize()) {
			DataMessageComponent despatch = null;
			try {
				despatch = getDespatch();
				if (despatch==null) return !isFinished;
				sendOutputMsg(output, MessageUtils.getDataMessage(despatch));
				
			} catch (ProcessingException pe) {
				throw pe;
			} catch (Exception ne) {
				logger.error("Cannot add data from '"+despatch+"'", ne);
			}
		}
		
		if (isFinished) return false;
		return true; // Wait for more
	}
	
	protected void doWrapUp() throws TerminationException {
		super.doWrapUp();
		if (isFinishRequested()) {
			cache.clear();
		}
	}

	
	protected DataMessageException createDataMessageException(String msg,Throwable e) throws DataMessageException {
		return new DataMessageException(msg, this, cache, e);
	}
	
	
	protected String getModelPath() {
		if (getContainer()==null) return null;
		final String source = getContainer().getSource();
		return source;
	}


	public Map<String, String> getExampleValues() {
		final Map<String,String> vars = new HashMap<String,String>(7);
		final List<IVariable>    variables = getInputVariables();
		for (int i = 0; i < variables.size(); i++) {
			final IVariable v = variables.get(i);
			if (v!=null) {
				final Object ev = v.getExampleValue();
				vars.put(v.getVariableName(), ev!=null?ev.toString():"");
			}
		}
		return vars;
	}

}
