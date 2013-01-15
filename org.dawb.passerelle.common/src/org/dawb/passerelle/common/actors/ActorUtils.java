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

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.dawb.workbench.jmx.UserDebugBean;
import org.dawb.workbench.jmx.UserDebugBean.DebugType;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanPropertyAction;
import org.eclipse.swt.SWT;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.Attribute;

import com.isencia.passerelle.actor.Actor;

public class ActorUtils {

	private static Logger logger = LoggerFactory.getLogger(ActorUtils.class);
	
	private static final String BREAKA = "_break_pointA";
	private static final String BREAKB = "_break_pointB";
	/**
	 * Creates a attribute(s) that can be used as break points.
	 * 
	 * @param actor
	 */
	public static void createDebugAttributes(final Actor actor) {
		
		try {
			final Parameter breakPointBefore = new Parameter(actor, BREAKA);
			breakPointBefore.setDisplayName("Break Point Before");
			breakPointBefore.setToken(new BooleanToken(false));
			actor.registerExpertParameter(breakPointBefore);
			
			
			final Parameter breakPointAfter = new Parameter(actor, BREAKB);
			breakPointAfter.setDisplayName("Break Point After");
			breakPointAfter.setToken(new BooleanToken(false));
			actor.registerExpertParameter(breakPointAfter);
		} catch (Exception ne) {
			logger.trace("Cannot create debugging attributes!", ne);
		}

	}
	
	/**
	 * 
	 * @param actor
	 * @param data
	 * @param type
	 * @return Return the bean if valid call, otherwise null.
	 */
	public static UserDebugBean create(final Actor actor, final DataMessageComponent data, final DebugType type) throws Exception {
		
		if (actor==null || data==null || type==null) return null;
		
		final boolean breakA = getBooleanValue(actor, BREAKA, false);
		final boolean breakB = getBooleanValue(actor, BREAKB, false);
		if (!breakA && !breakB) return null;
		
		if (breakA && type==DebugType.AFTER_ACTOR)  return null;
		if (breakB && type==DebugType.BEFORE_ACTOR) return null;
		
		UserDebugBean bean = new UserDebugBean();
		bean.setActorName(actor.getDisplayName());
		bean.setDataMessageComponent(data);
		bean.setDebugType(type);
		bean.setSilent(false);
		
		return bean;
	}
	
	/**
	 * Blocks until user presses the continue button if a debug parameter is set.
	 * 
	 * @param bean
	 * @return
	 */
	public static UserDebugBean debug(UserDebugBean bean) {

		if (bean==null) return null;
		try {		
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection(500);
			if (client==null) return null;
						
			bean = (UserDebugBean)client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "debug", new Object[]{bean}, new String[]{UserDebugBean.class.getName()});
		
			return bean;
		} catch (Exception ignored) {
			logger.trace("Cannot debug", ignored);
			return null;
		}
		
	}
	
	/**
	 * Method to deal with the horrible way in which ptolmey stores boolean values.
	 * 
	 * @param actor
	 * @param name
	 * @param defaultValue
	 * @return
	 * @throws Exception
	 */
	private static boolean getBooleanValue(Actor actor, String name, boolean defaultValue) throws Exception {
		final Attribute att = actor.getAttribute(name);
		if (att==null) return defaultValue;
		if (!(att instanceof Parameter)) return defaultValue;
		Token tok = ((Parameter)att).getToken();
		if (!(tok instanceof BooleanToken)) return defaultValue;
		return ((BooleanToken)tok).booleanValue();
		// All this just to get a boolean! 
	}

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
		return getWorkbenchConnection(1000);
	}
	/**
	 * Designed to avoid too many timeouts if workbench not there.
	 * @return
	 * @throws Exception
	 */
	public static MBeanServerConnection getWorkbenchConnection(int timeoutMs) throws Exception {
		
		if (isWorkbenchPresentChecked) return workbenchConnection;
		
		workbenchConnection       = RemoteWorkbenchAgent.getServerConnection(timeoutMs);
		isWorkbenchPresentChecked = true;
		
		return workbenchConnection;
	}
}
