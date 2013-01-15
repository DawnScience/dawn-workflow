/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx;

import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;


/**
 * Service run by the workflow to allow the RCP workbench
 * to interfact with the service.
 * 
 * @author gerring
 *
 */
public class RemoteWorkbenchManager extends StandardMBean implements RemoteWorkbenchMBean {

	private static final String START_CODE         = "org.dawb.workbench.jmx.exec.started";
	private static final String TERM_CODE          = "org.dawb.workbench.jmx.exec.terminated";
	private static final String OPEN_CODE          = "org.dawb.workbench.jmx.openFile";
	private static final String MOCK_MOTOR_CODE    = "org.dawb.workbench.jmx.mockMotor";
	private static final String MOCK_NOTIFY_CODE   = "org.dawb.workbench.jmx.mockNotifyCmd";
	private static final String MONITOR_CODE       = "org.dawb.workbench.jmx.monitorDir";
	private static final String REFRESH_CODE       = "org.dawb.workbench.jmx.refreshProject";
	private static final String MESSAGE_CODE       = "org.dawb.workbench.jmx.showMessage";
	private static final String LOG_CODE           = "org.dawb.workbench.jmx.logStatus";
	private static final String EDIT_CODE          = "org.dawb.workbench.jmx.editScalarValues";
	private static final String PLOT_CODE          = "org.dawb.workbench.jmx.plotInputValues";
	private static final String ACTOR_SELECTED_CODE= "org.dawb.workbench.jmx.actorSelectedCode";
	private static final String DEBUG_CODE         = "org.dawb.workbench.jmx.debugCode";
	
	
	private final IRemoteWorkbench       rmDelegate;

	public RemoteWorkbenchManager(final IRemoteWorkbench bench) throws Exception {
		
		super(RemoteWorkbenchMBean.class);
		
		this.rmDelegate = bench;
	}

	private NotificationBroadcasterSupport generalBroadcaster;
	
	private void sendNotification(String code) {
		this.sendNotification(code, null);
	}
	
	private void sendNotification(String code, Object userObject) {
		if (generalBroadcaster!= null) {
			final Notification notification = new Notification(code, this, -1);
			notification.setUserData(userObject);
			generalBroadcaster.sendNotification(notification);
		}	
	}

	/**
	 * Call with full file path on visible disk.
	 */
	@Override
	public Object getMockMotorValue(final String name) {
		
		final Object value = rmDelegate.getMockMotorValue(name);
		sendNotification(MOCK_MOTOR_CODE);
     
		return value;
	}

	/**
	 * Call with full file path on visible disk.
	 */
	@Override
	public void setMockMotorValue(final String name, final Object value) {
		
		rmDelegate.setMockMotorValue(name, value);
		sendNotification(MOCK_MOTOR_CODE);
     
	}
	/**
	 * Call with full file path on visible disk.
	 */
	@Override
	public void notifyMockCommand(final String name, final String message, final String cmd) {
		
		rmDelegate.notifyMockCommand(name, message, cmd);
		sendNotification(MOCK_NOTIFY_CODE);
     
	}

	/**
	 * 
	 */
	public void executionStarted() {
		rmDelegate.executionStarted();
		sendNotification(START_CODE);
	}
	
	/**
	 * 
	 */
	public void executionTerminated(final int returnCode) {
		rmDelegate.executionTerminated(returnCode);
		sendNotification(TERM_CODE);
	}
	
	/**
	 * Call with full file path on visible disk.
	 */
	@Override
	public boolean openFile(final String fullPath) {
		
		final boolean opened = rmDelegate.openFile(fullPath);
		if (opened) sendNotification(OPEN_CODE);
        return opened;
	}
	
	/**
	 * Call with full file path on visible disk.
	 */
	@Override
	public boolean monitorDirectory(final String fullPath, final boolean startMonitoring) {
		
		final boolean opened = rmDelegate.monitorDirectory(fullPath, startMonitoring);
		if (opened) sendNotification(MONITOR_CODE);
        return opened;
	}
	
	@Override
	public boolean refresh(final String projectName, final String resourcePath) {
		
		final boolean refreshed = rmDelegate.refresh(projectName, resourcePath);
		if (refreshed) sendNotification(REFRESH_CODE);
    	return refreshed;
	}
	
	@Override
	public boolean showMessage(final String title, final String message, final int type) {
		
		boolean showed = rmDelegate.showMessage(title, message, type);
		sendNotification(MESSAGE_CODE);
    	return showed;
	}
	
	@Override
	public void logStatus(final String pluginId, final String message, final Throwable throwable) {
		
		rmDelegate.logStatus(pluginId, message, throwable);
		sendNotification(LOG_CODE);
    	return;
	}
	
	@Override
	public Map<String,String> createUserInput(final UserInputBean bean) throws Exception {
		
		Map<String,String> newValues = rmDelegate.createUserInput(bean);
		sendNotification(EDIT_CODE);
    	return newValues;
	}
	
	@Override
	public UserPlotBean createPlotInput(final UserPlotBean bean) throws Exception {
		
		UserPlotBean output = rmDelegate.createPlotInput(bean);
		sendNotification(PLOT_CODE);
    	return output;
	}

	@Override
	public UserDebugBean debug(final UserDebugBean bean) throws Exception {
		
		UserDebugBean output = rmDelegate.debug(bean);
		sendNotification(DEBUG_CODE);
    	return output;
	}

	@Override
	public boolean setActorSelected(final String  resourcePath, 
			                        final String  actorName,
			                        final boolean isSelected,
			                        final int     colorCode) throws Exception {
        
		boolean found = rmDelegate.setActorSelected(resourcePath, actorName, isSelected,colorCode);
		sendNotification(ACTOR_SELECTED_CODE);
    	return found;
	}
	
	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
		
		if (generalBroadcaster == null)  generalBroadcaster = new NotificationBroadcasterSupport();		
		generalBroadcaster.addNotificationListener(listener, filter, handback);
	}

	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] {
				new MBeanNotificationInfo(
						new String[] { OPEN_CODE },   // notif. types
						Notification.class.getName(), // notif. class
						"User Notifications."         // description
				)
		};
	}

	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		
		if (generalBroadcaster == null) throw new ListenerNotFoundException("No notification listeners registered");
		generalBroadcaster.removeNotificationListener(listener);
	}

}
