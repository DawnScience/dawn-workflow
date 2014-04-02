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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.message.ManagedMessage;

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
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractDataMessageTransformer.class);

	protected DataMessageComponent dataMsgComp = null;

	public AbstractDataMessageTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}
	
	protected abstract DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException;
	
	@Override
	protected void process(ActorContext ctxt, ProcessRequest request,
			ProcessResponse response) throws ProcessingException {
		ManagedMessage message = request.getMessage(input);
		dataMsgComp = null;
		try {
			if (message!=null) {
				dataMsgComp = MessageUtils.coerceMessage(message);
				final DataMessageComponent despatch = getDespatch(dataMsgComp);
				if (despatch==null) return;
				
        response.addOutputMessage(output, MessageUtils.getDataMessage(despatch, message));
			}
		} catch (ProcessingException pe) {
			throw new ProcessingException(pe.getErrorCode(), pe.getMessage(), pe.getModelElement(), message, pe.getCause());
		} catch (Exception ne) {
			throw new DataMessageException(ErrorCode.ERROR, "Cannot add data", this, message, dataMsgComp, ne);
		}
	}
	
	private DataMessageComponent getDespatch(DataMessageComponent dataMsgComp) throws ProcessingException {
		
		try {
			ActorUtils.setActorExecuting(this, true);

			List<DataMessageComponent> cache = Arrays.asList(dataMsgComp);
			final DataMessageComponent despatch = getTransformedMessage(cache);
			if (despatch!=null) setDataNames(despatch, cache);
			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(cache), despatch);
				if (bean!=null) bean.setPortName(input.getDisplayName());
				ActorUtils.debug(this, bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}
			if (despatch==null) return null;
			
			despatch.putScalar("operation.time."+getName(), DateFormat.getDateTimeInstance().format(new Date()));
			despatch.putScalar("operation.type."+getName(), getOperationName());
			return despatch;
			
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}
	

	
	protected DataMessageException createDataMessageException(String msg,Throwable e) throws DataMessageException {
		return new DataMessageException(msg, this, dataMsgComp, e);
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
