/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent for remote workbench
 * 
 * @author fcp94556
 *
 */
public class RemoteWorkbenchAgent {

	public static       ObjectName    REMOTE_WORKBENCH;
	
	private static Logger logger = LoggerFactory.getLogger(RemoteWorkbenchAgent.class);
	
	private JMXServiceURL serverUrl;
	private int           currentPort;

	private RemoteWorkbenchMBean        remoteManager;
    private static RemoteWorkbenchAgent instance;
		
	public RemoteWorkbenchAgent(final IRemoteServiceProvider service) throws Exception {
		
		IRemoteWorkbench bench = service.getRemoteWorkbench();
		//if (bench==null) bench = OSGIUtils.getRemoteWorkbench();
		this.remoteManager = new RemoteWorkbenchManager(bench);
		createServerUrl(service.getStartPort());
		instance = this;
	}
	
	public static RemoteWorkbenchAgent getInstance() {
		return instance;
	}
	
	private final void createServerUrl(final int startPort) {
		
		if (serverUrl!=null) return;
		try {
			String hostName = System.getProperty("org.dawb.workbench.jmx.host.name");
			if (hostName==null) hostName = InetAddress.getLocalHost().getHostName();
			if (hostName==null) hostName = InetAddress.getLocalHost().getHostAddress();
			if (hostName==null) hostName = "localhost";
			
			currentPort      = getFreePort(startPort);
			serverUrl        = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+hostName+":"+currentPort+"/workbench");
			REMOTE_WORKBENCH = new ObjectName(RemoteWorkbenchManager.class.getPackage().getName()+":type=RemoteWorkbench");
		} catch (Exception e) {
			logger.error("Cannot create ObjectName for remotemanager", e);
		}
		
		logger.debug("Workbench URI: "+serverUrl.getURLPath());

	}
	
	public int getCurrentPort() {
		return currentPort;
	}
	
	/**
	 * Call this method to start the agent which will deploy the
	 * service on JMX.
	 */
	public void start() throws Exception {

		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

		// We force a new registry on the port and use this
		// for workflow processes started.
		try {
			LocateRegistry.createRegistry(currentPort);
		} catch (java.rmi.server.ExportException ne) {
			// If we are running in tango server mode, there may be a registry already existing.
			logger.debug("Found existing registry on "+currentPort);
		}

		// Uniquely identify the MBeans and register them with the MBeanServer 
		try {
			if (mbs.getObjectInstance(REMOTE_WORKBENCH)!=null) {
				mbs.unregisterMBean(REMOTE_WORKBENCH);
			}
		} catch (Exception ignored) {
			// Throws exception not returns null, so ignore.
		}
		mbs.registerMBean(remoteManager, REMOTE_WORKBENCH);

		// Create an RMI connector and start it
		JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(serverUrl, null, mbs);
		cs.start();

		logger.debug("Workbench service started on "+serverUrl);
	}


	public void stop() throws Exception {
		
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

		if (mbs.getObjectInstance(REMOTE_WORKBENCH)!=null) {
			mbs.unregisterMBean(REMOTE_WORKBENCH);
			logger.debug("Workbench service stopped on "+serverUrl);
		}


		final Registry reg = LocateRegistry.getRegistry(getCurrentPort());
		if (reg.lookup("workbench") != null) {
			reg.unbind("workbench");
		}


	}

	private static int getFreePort(final int startPort) {
		
		int jmxServicePort = 21701;
		
		jmxServicePort =  startPort;
	    if (NetUtils.isPortFree(jmxServicePort)) {
	    	System.setProperty("com.isencia.jmx.service.port", jmxServicePort+"");
			logger.debug("Used free port at "+jmxServicePort);
	    	return jmxServicePort;
	    }
	    	
	    while(!NetUtils.isPortFree(jmxServicePort)) jmxServicePort++;
	    	
    	System.setProperty("com.isencia.jmx.service.port", jmxServicePort+"");
		logger.debug("Assigned free port at "+jmxServicePort);
	    return jmxServicePort;
	}

}
