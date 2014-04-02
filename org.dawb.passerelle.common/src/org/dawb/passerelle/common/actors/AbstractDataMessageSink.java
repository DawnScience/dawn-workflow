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

import java.util.Arrays;
import java.util.List;

import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;
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
	
	private DataMessageComponent dataMsgComp;
	
	public AbstractDataMessageSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}


	@Override
	protected void sendMessage(ManagedMessage message) throws ProcessingException {
		try {
			dataMsgComp = MessageUtils.coerceMessage(message);
			List<DataMessageComponent> cache = Arrays.asList(dataMsgComp);

			if (message instanceof ErrorMessageContainer) {
				sendCachedDataInternal(cache);
				requestFinish();
				
			} else {
				sendCachedDataInternal(cache);
			}
			
		} catch (ProcessingException pe) {
			throw new ProcessingException(pe.getErrorCode(), pe.getMessage(), pe.getModelElement(), message, pe.getCause());
		} catch (Exception ne) {
			throw createDataMessageException("Cannot add data from '"+message+"'", ne);
		}
	}
	
	private void sendCachedDataInternal(List<DataMessageComponent> cache) throws ProcessingException {
		try {
			ActorUtils.setActorExecuting(this, true);
			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(cache) );
				if (bean!=null) bean.setPortName(input.getDisplayName());
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

	
	
	protected DataMessageException createDataMessageException(String msg,Throwable e) throws DataMessageException {
		return new DataMessageException(msg, this, dataMsgComp, e);
	}
	
}
