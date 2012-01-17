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

import javax.management.MBeanServerConnection;

import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isencia.passerelle.actor.Actor;

public class ActorUtils {

	private static Logger logger = LoggerFactory.getLogger(ActorUtils.class);
	
	/**
	 * Tells the user interface that a given actor is running.
	 * 
	 * Call with try finally.
	 * 
	 * @param actor
	 * @param isExecuting
	 * @throws Exception 
	 */
	public static void setActorExecuting(final Actor actor, final boolean isExecuting) {
		
		if (actor.getManager()== null) return;
		
		if (Platform.getBundle("org.dawb.workbench.ui")!=null) {
			final ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,"org.dawb.workbench.ui");
			final boolean isSel = store.getBoolean("org.dawb.actor.highlight.choice");
			if (!isSel) return;
		}

		try {
			//TODO Check property to see if actor highlighting is switched on.
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			if (client==null) return;
			
			client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{actor.getContainer().getSource(), actor.getName(), isExecuting, SWT.COLOR_BLUE}, new String[]{String.class.getName(), String.class.getName(), boolean.class.getName(), int.class.getName()});
		
		} catch (Exception ignored) {
			logger.trace("Cannot set actor selected", ignored);
			return;
		}
	}

	private static MBeanServerConnection workbenchConnection;
	private static boolean               isWorkbenchPresentChecked=false;
	
	/**
	 * Designed to avoid too many timeouts if workbench not there.
	 * @return
	 * @throws Exception
	 */
	public static MBeanServerConnection getWorkbenchConnection() throws Exception {
		
		if (isWorkbenchPresentChecked) return workbenchConnection;
		
		workbenchConnection       = RemoteWorkbenchAgent.getServerConnection(1000);
		isWorkbenchPresentChecked = true;
		
		return workbenchConnection;
	}
}
