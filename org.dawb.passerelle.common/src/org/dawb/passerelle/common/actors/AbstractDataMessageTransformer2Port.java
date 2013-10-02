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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortMode;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageInputContext;

/**
 * A Transformer which gets its data messages from the normal input port, and one modifier message
 * from a second input port. This modifier message can be used in operations on the data messages.
 * <p>
 * E.g. data messages can contain images read from a folder, and the modifier message can contain 
 * a background image that can then be subtracted from each data message.
 * </p>
 * 
 * @author gerring
 *
 */
public abstract class AbstractDataMessageTransformer2Port extends AbstractPassModeTransformer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6609512817357858738L;
	
	
	/**
	 *  NOTE Ports must be public for composites to work.
	 */
    public  Port           inputPort2;
    
    private DataMessageComponent modifierDataMsgComp;

	protected static final Logger logger = LoggerFactory.getLogger(AbstractDataMessageTransformer2Port.class);

	public AbstractDataMessageTransformer2Port(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		input.setName("a");
		inputPort2 = PortFactory.getInstance().createInputPort(this, "b", PortMode.PUSH, null);
	}
	

	protected abstract DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException;
	
	@Override
	public void offer(MessageInputContext ctxt) throws PasserelleException {
		super.offer(ctxt);
		if(inputPort2.getName().equals(ctxt.getPortName())) {
			try {
				modifierDataMsgComp = MessageUtils.coerceMessage(ctxt.getMsg());
			} catch (Exception e) {
				throw new PasserelleException(
						ErrorCode.MSG_CONTENT_TYPE_ERROR, this, e);
			}
		}
	}
	
	@Override
	protected boolean doPreFire() throws ProcessingException {
		return super.doPreFire() && (modifierDataMsgComp!=null);
	}
	
	@Override
	protected void process(ActorContext ctxt, ProcessRequest request,
			ProcessResponse response) throws ProcessingException {
		ManagedMessage message = request.getMessage(input);
		DataMessageComponent dataMsgComp = null;
		try {
			if (message!=null) {
				dataMsgComp = MessageUtils.coerceMessage(message);
				final DataMessageComponent despatch = getDespatch(dataMsgComp, modifierDataMsgComp);
				if (despatch==null) return;
				sendOutputMsg(output, MessageUtils.getDataMessage(despatch, message));
			}
		} catch (ProcessingException pe) {
			throw new ProcessingException(pe.getErrorCode(), pe.getMessage(), pe.getModelElement(), message, pe.getCause());
		} catch (Exception ne) {
			throw new DataMessageException(ErrorCode.ERROR, "Cannot add data", this, message, dataMsgComp, ne);
		}
	}
	
	private DataMessageComponent getDespatch(DataMessageComponent dataMsgComp, DataMessageComponent modifierDataMsgComp) throws ProcessingException {
		
		try {
			ActorUtils.setActorExecuting(this, true);
			
			if (dataMsgComp.isScalarOnly()) {
				throw new DataMessageException(ErrorCode.ERROR, "Cannot send messages with scalar data only to port '" +input.getName()+ "' of '"+getName()+"'", this, null, dataMsgComp, null);
			}
			
			List<DataMessageComponent> port1Cache = Arrays.asList(dataMsgComp);
			List<DataMessageComponent> port2Cache = Arrays.asList(modifierDataMsgComp);
			final DataMessageComponent despatch = getTransformedMessage(port1Cache , MessageUtils.mergeScalar(port2Cache));
			if (despatch!=null) setDataNames(despatch, port1Cache);

			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(port1Cache), despatch);
				if (bean!=null) bean.setPortName(input.getDisplayName());
				ActorUtils.debug(this, bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}

			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(port2Cache), despatch);
				if (bean!=null) bean.setPortName(inputPort2.getDisplayName());
				ActorUtils.debug(this, bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}
			

			port1Cache.clear();
			if (despatch==null) return null;
			
			despatch.putScalar("operation.time."+getName(), DateFormat.getDateTimeInstance().format(new Date()));
			despatch.putScalar("operation.type."+getName(), getOperationName());

			return despatch;
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}
	

	protected void setUpstreamValues(final DataMessageComponent ret,
									final List<DataMessageComponent> port1Cache,
									final List<DataMessageComponent> port2Cache) {
		
		ret.setMeta(MessageUtils.getMeta(port1Cache));
		ret.addScalar(MessageUtils.getScalar(port1Cache));
		ret.addScalar(MessageUtils.getScalar(port2Cache), false);
	}
}
