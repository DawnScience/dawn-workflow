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

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.dawb.workbench.jmx.UserDebugBean.DebugType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.internal.ErrorMessageContainer;

/**
 * This sink receives data sets and 
 * @author gerring
 *
 */
public abstract class AbstractDataMessageSink extends AbstractPassModeSink {

	private static Logger logger = LoggerFactory.getLogger(AbstractDataMessageSink.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -8638005806521694925L;
	
	private List<DataMessageComponent> cache;
	private boolean firedOnce=false;
	
	public AbstractDataMessageSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		cache = new ArrayList<DataMessageComponent>(7);
	}

	public void doPreInitialize() {
		cache.clear();
	}

	@Override
	protected void sendMessage(ManagedMessage message) throws ProcessingException {
		try {
			final DataMessageComponent comp = MessageUtils.coerceMessage(message);
			if (isFireOnce()) {
				if (!firedOnce) {
					cache.add(comp);
					sendCachedDataInternal(cache);
					firedOnce = true;
				} else {
					cache.clear();
					return;
				}
			}
			
			cache.add(comp);
			
			if (message instanceof ErrorMessageContainer) {
				sendCachedDataInternal(cache);
				cache.clear();
				requestFinish();
				
			} else if (isFireInLoop()) {
				sendCachedDataInternal(cache);
				cache.clear();
				
			}
			
		} catch (ProcessingException pe) {
			throw pe;
		} catch (Exception ne) {
			throw createDataMessageException("Cannot add data from '"+message+"'", ne);
		}
	}
	
	private void sendCachedDataInternal(List<DataMessageComponent> cache) throws ProcessingException {
		try {
			ActorUtils.setActorExecuting(this, true);
			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(cache), DebugType.BEFORE_ACTOR);
				ActorUtils.debug(this, bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}
			sendCachedData(cache);
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}
	
	protected abstract void sendCachedData(List<DataMessageComponent> cache) throws ProcessingException;

	protected boolean doPostFire() throws ProcessingException {
		
		final boolean isFinished   = isFinishRequested();
		final boolean isInputRound = isInputRoundComplete();
		
		if ((isInputRound && isFireEndLoop()) || (isFinished && isFireOnFinished())) {
			try {
				if (!cache.isEmpty()) {
					sendCachedDataInternal(cache);
				}

			} catch (ProcessingException pe) {
				throw pe;
			} catch (Exception ne) {
				logger.error("Cannot process data", ne);
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
	
}
