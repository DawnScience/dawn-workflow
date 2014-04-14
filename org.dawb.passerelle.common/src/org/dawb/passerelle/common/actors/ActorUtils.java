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

import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServerConnection;

import org.dawb.common.services.IClassLoaderService;
import org.dawb.passerelle.common.Activator;
import org.dawb.workbench.jmx.ActorSelectedBean;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.Attribute;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

public class ActorUtils {

	private static Logger logger = LoggerFactory.getLogger(ActorUtils.class);
	
	private static final String BREAKA = "_break_point";
	/**
	 * Creates a attribute(s) that can be used as break points.
	 * 
	 * @param actor
	 */
	public static void createDebugAttribute(final IProjectNamedObject actor) {
		
		try {
			final Parameter breakPoint = new Parameter(actor.getObject(), BREAKA);
			breakPoint.setDisplayName("Break Point");
			breakPoint.setToken(new BooleanToken(false));
			actor.registerExpertParameter(breakPoint);
			
		} catch (Exception ne) {
			logger.trace("Cannot create debugging attribute!", ne);
		}

	}
	
	/**
	 * 
	 * @param actor
	 * @param data
	 * @param type
	 * @return Return the bean if valid call, otherwise null.
	 */
	public static UserDebugBean create(final IProjectNamedObject actor, final DataMessageComponent... data) throws Exception {
		
		if (Boolean.getBoolean("org.dawb.passerelle.common.actors.util.bypassJMX")) return null;
		if (actor==null || data==null || data.length<1) return null;
		
		final boolean breakA = getBooleanValue(actor, BREAKA, false);
		if (!breakA) return null;
				
		UserDebugBean bean = new UserDebugBean();
		bean.setActorName(actor.getDisplayName());

		if (data[0]!=null) {
			bean.setScalar(data[0].getScalar());
			bean.setData(data[0].getList());
			bean.setRois(data[0].getRois());
		}
		
		if (data.length==2) {
			bean.setOutputScalar(data[1].getScalar());
			bean.setOutputData(data[1].getList());
			bean.setOutputRois(data[1].getRois());
		}
		
		bean.setSilent(false);
		
		return bean;
	}
	
	private static ReentrantLock debugLock = new ReentrantLock();
	/**
	 * Blocks until user presses the continue button if a debug parameter is set.
	 * 
	 * Blocks also if another actor is being debugged. Only one active debug call
	 * can be done at a time.
	 * 
	 * @param bean
	 * @return
	 */
	public static UserDebugBean debug(IProjectNamedObject actor, UserDebugBean bean) {

		if (Boolean.getBoolean("org.dawb.passerelle.common.actors.util.bypassJMX")) return null;
		if (bean==null)            return null;

		try {
			debugLock.lock();
			
			bean.addScalar("project_name", actor.getProject().getName());
			bean.addOutputScalar("project_name", actor.getProject().getName());
			
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection(500);
			if (client==null) return null;
			
			IClassLoaderService service = (IClassLoaderService)Activator.getService(IClassLoaderService.class);
			
			ActorSelectedBean selBean = new ActorSelectedBean(actor.getContainer().getSource(), actor.getName(), true, 11/**SWT.COLOR_MAGENTA**/);
			selBean.setPortName(bean.getPortName());
			selBean.setPortColorCode(11/**SWT.COLOR_MAGENTA**/);
			try {
				if (service!=null) service.setDataAnalysisClassLoaderActive(true);
					
				// Highlight it as being debugged.
				client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{selBean}, new String[]{ActorSelectedBean.class.getName()});
	
				bean = (UserDebugBean)client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "debug", new Object[]{bean}, new String[]{UserDebugBean.class.getName()});
				
				if (bean==null){
					return null;
				} else if (bean.isEmpty()) {
					actor.requestFinish();
					actor.getManager().stop();
					return null;
				}
				
				return bean;
				
			} catch (Exception ignored) {
				logger.trace("Cannot debug", ignored);
				return null;
			
			} finally {
				if (service!=null) service.setDataAnalysisClassLoaderActive(false);
				
				try {
					selBean.setSelected(false);
				    client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{selBean}, new String[]{ActorSelectedBean.class.getName()});
				} catch (Exception ne) {
					logger.trace("Cannot set actor back to non-executing!", ne);
				}
			}
		} catch (Exception ne) {
			logger.error("Cannot get workbench connection!", ne);
			return null;
		} finally {
			
			debugLock.unlock();

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
	private static boolean getBooleanValue(IProjectNamedObject actor, String name, boolean defaultValue) throws Exception {
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
	public static void setActorExecuting(final IProjectNamedObject actor, final boolean isExecuting) {
		
		if (Boolean.getBoolean("org.dawb.passerelle.common.actors.util.bypassJMX")) return;
		if (actor.getManager()== null) return;
		
		if (Platform.getBundle("org.dawb.workbench.ui")!=null) {
			IPreferencesService service = Platform.getPreferencesService();
			Preferences store = service.getRootNode().node(InstanceScope.SCOPE).node("org.dawb.workbench.ui");
			final boolean isSel = store.getBoolean("org.dawb.actor.highlight.choice", true);
			if (!isSel) return;
		}

		try {
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			if (client==null) return;
			
			final ActorSelectedBean bean = new ActorSelectedBean(actor.getContainer().getSource(), actor.getName(), isExecuting, 9 /**SWT.COLOR_BLUE**/);
			client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "setActorSelected", new Object[]{bean}, new String[]{ActorSelectedBean.class.getName()});
		
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
	
	/**
	 * A boolean to record if sources should wait which they may optionally check.
	 * This boolean is separate to pausing the whole manager because for some reason
	 * passerelle does not unpause when the resume method is called.
	 * 
	 * This pausing almost certainly cannot be used with the event model.
	 */
	private static ReentrantLock paused = new ReentrantLock();
	
	/**
	 * Must call from try finally.
	 * @param lock
	 */
	public static void setLocked(boolean lock) {
		if (lock) {
			paused.lock();
		} else {
			paused.unlock();
		}
	}
	
	public static void waitWhileLocked() {
		try {
		    paused.lock();
		    paused.unlock();
		} catch (IllegalMonitorStateException ime) {
			return; // It got locked by another thread on the setPaused but that is ok.
		}
	}
}
