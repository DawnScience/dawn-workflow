/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteWorkflow {

	public static       ObjectName    REMOTE_MANAGER;
	
	private static Logger logger = LoggerFactory.getLogger(RemoteWorkflow.class);
	static {
		try {
			REMOTE_MANAGER = new ObjectName("com.isencia.passerelle.workbench.model.jmx:type=RemoteManager");
		} catch (Exception e) {
			logger.error("Cannot create ObjectName for remotemanager", e);
		}
	}
	
	private static final String getHostName() throws UnknownHostException {
		String hostName = System.getProperty("org.dawb.workbench.jmx.host.name");
		if (hostName==null) hostName = InetAddress.getLocalHost().getHostName();
		if (hostName==null) hostName = InetAddress.getLocalHost().getHostAddress();
		if (hostName==null) hostName = "localhost";
		return hostName;
	}

	/**
	 * The system property "com.isencia.jmx.service.port" should have been set
	 * by the workbench activator before this is called.
	 * 
	 * There must be a registry started on this port and defined with "com.isencia.jmx.service.port"
	 * 
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static MBeanServerConnection getServerConnection(final long timeout) throws Exception {

		if (System.getProperty("com.isencia.jmx.service.port")==null) {
			throw new Exception("You must start the registry before calling this method and set the property 'com.isencia.jmx.service.port'");
		}
		
		long                  waited = 0;
		MBeanServerConnection server = null;
		
		while(timeout>waited) {
			
			waited+=100;
			try {
				final String hostName       = getHostName();
				final int     port          = Integer.parseInt(System.getProperty("com.isencia.jmx.service.port"));
				JMXServiceURL serverUrl     = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+hostName+":"+port+"/workflow");
				JMXConnector  conn = JMXConnectorFactory.connect(serverUrl);
				server             = conn.getMBeanServerConnection();
                if (server == null) throw new NullPointerException("MBeanServerConnection is null");
				break;
                
			} catch (Throwable ne) {
				if (waited>=timeout) {
					throw new Exception("Cannot get connection. Connection took longer than "+timeout, ne);
				} else {
					Thread.sleep(100);
					continue;
				}
			}
		}
		return server;
	}
}
