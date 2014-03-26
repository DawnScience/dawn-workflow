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

import org.dawb.passerelle.common.actors.AbstractSource;
import org.dawb.passerelle.common.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * A very simple source actor which creates an empty data message
 * 
 * @author svensson
 *
 */
public class Start extends AbstractSource {

	private static final long serialVersionUID = 850115774817072294L;
	private static final Logger logger = LoggerFactory.getLogger(Start.class);	
	private boolean hasSentMessage = false;
	
	public Start(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}

	@Override
	protected ManagedMessage getMessage() throws ProcessingException {

//		logger.debug("Creating an empty start data message");
		
		DataMessageComponent despatch = new DataMessageComponent();
		ManagedMessage startMessage = null;

		try {
			startMessage = MessageUtils.getDataMessage(despatch, null);
		} catch (Exception e) {
			// TODO: throw processing exceptions
			e.printStackTrace();
		}
		hasSentMessage = true;
		
		return startMessage;
	}	

	public boolean hasNoMoreMessages() {
		return hasSentMessage;
	}
}