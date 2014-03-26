/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.Actor;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;

public class PassDelay extends AbstractDataMessageTransformer implements Actor {

	private Logger logger = LoggerFactory.getLogger(PassDelay.class);
	
	private final Parameter timeParameter;
	/**
	 * 
	 */
	private static final long serialVersionUID = -6096875966009208199L;

	public PassDelay(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		timeParameter = new Parameter(this, "time(s)", new IntToken(1));
		timeParameter.setTypeEquals(BaseType.DOUBLE);
        registerConfigurableParameter(timeParameter);
        
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
		
	}


	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		

		double time = 0d;
		try {
			time = ((DoubleToken) timeParameter.getToken()).doubleValue();
			if (time>0) {
				logger.debug("Delay action, sleeping for "+time+" seconds.");
				Thread.sleep(Math.round(time * 1000d));
			}
			
		} catch(InterruptedException e) {
            // do nothing, means someone wants us to stop
			time = 0d;
        } catch (Exception e) {
			throw createDataMessageException("Cannot sleep for '"+timeParameter.getExpression()+"' in "+getName(),e);
		}
        
        DataMessageComponent pass = MessageUtils.copy(cache);
        pass.putScalar("sleep_time", ""+time);
        return pass;
	}

	@Override
	protected String getOperationName() {
		return "Sleep";
	}

	@Override
	protected String getExtendedInfo() {
		return "Sleeps for a time defined in seconds.";
	}
	
	@Override
	public List<IVariable> getOutputVariables() {
        return getInputVariables();
	}
}
