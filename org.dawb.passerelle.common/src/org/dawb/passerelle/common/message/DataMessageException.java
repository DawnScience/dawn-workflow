/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawb.passerelle.common.message;

import java.util.Collection;

import javax.management.MBeanServerConnection;

import org.dawb.passerelle.common.Activator;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;

import ptolemy.kernel.util.NamedObj;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;

/**
 * This class is used to broadcast a ProcessingException with the
 * DataMessageComponent. It does not have to be used when sending a
 * ProcessingException, however if it is, actors connecting to the error port
 * such as the MessageSink can expand values from the message in their error
 * messages.
 * 
 * @author Matthew Gerring
 * 
 */
public class DataMessageException extends ProcessingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7005072679084823511L;
	private DataMessageComponent dataMessageComponent;

	public DataMessageException(String message, Object context,
			Throwable rootException) {
		this(message, context, new DataMessageComponent(), rootException);
	}

	public DataMessageException(String message, Object context,
			Collection<DataMessageComponent> sets, Throwable rootException) {
		this(message, context, MessageUtils.mergeAll(sets), rootException);
	}

	public DataMessageException(String message, Object context,
			DataMessageComponent comp, Throwable rootException) {
		this(ErrorCode.ERROR, message, (NamedObj) context, DataMessageException
				.createManagedMeassage((Actor) context, comp), comp,
				rootException);

	}

	/**
	 * 
	 * @param errorCode
	 * @param message
	 * @param modelElement
	 * @param msgContext
	 * @param rootException
	 */
	public DataMessageException(ErrorCode errorCode, String message,
			NamedObj modelElement, ManagedMessage msgContext,
			DataMessageComponent comp, Throwable rootException) {
		
		super(errorCode, message, modelElement, msgContext, rootException);
		this.dataMessageComponent = comp;
		dataMessageComponent.putScalar("message_text", "Error when executing actor \"" + modelElement.getName() + "\":\n" + message);
		dataMessageComponent.setError(true);
		if (rootException != null) {
			dataMessageComponent.putScalar("exception_text", rootException
					.getMessage() != null ? rootException.getMessage()
					: rootException.getClass().getName());
		}
		
		try { // Send this to the workbench log so that it is visible in the log
			// view.
			final MBeanServerConnection client = RemoteWorkbenchAgent.getServerConnection(1000);
			client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "logStatus",
						  new Object[] {Activator.PLUGIN_ID, message, rootException },
						  new String[] { String.class.getName(), String.class.getName(), Throwable.class.getName() });
			
		} catch (Throwable ignored) {
			// Nevermind then!
		}
	}

	public DataMessageComponent getDataMessageComponent() {
		return dataMessageComponent;
	}

	public void setDataMessageComponent(
			DataMessageComponent dataMessageComponent) {
		this.dataMessageComponent = dataMessageComponent;
	}

	/***
	 * This is a helper method for creating a ManagedMessage from a
	 * DataMessageComponent.
	 * 
	 * @param actor
	 * @param comp
	 */
	private static ManagedMessage createManagedMeassage(Actor actor,
			DataMessageComponent comp) {
		ManagedMessage managedMessage = actor.createMessage();
		try {
			managedMessage.setBodyContent(comp, "Incoming data");
		} catch (MessageException e1) {
			e1.printStackTrace();
		}
		return managedMessage;
	}

}
