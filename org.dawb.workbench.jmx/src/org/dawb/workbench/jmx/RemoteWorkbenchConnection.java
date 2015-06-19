package org.dawb.workbench.jmx;

import java.net.InetAddress;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Connection to remote workbench.
 * @author fcp94556
 *
 */
public class RemoteWorkbenchConnection {
	
	private static final Logger logger = LoggerFactory.getLogger(RemoteWorkbenchConnection.class);

	private static JMXServiceURL serverUrl;
	private static int           currentPort;

	
	private final static void createServerUrl(final int startPort) {
		
		if (serverUrl!=null) return;
		try {
			String hostName = System.getProperty("org.dawb.workbench.jmx.host.name");
			if (hostName==null) hostName = InetAddress.getLocalHost().getHostName();
			if (hostName==null) hostName = InetAddress.getLocalHost().getHostAddress();
			if (hostName==null) hostName = "localhost";
			
			currentPort      = getFreePort(startPort);
			serverUrl        = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+hostName+":"+currentPort+"/workbench");
		} catch (Exception e) {
			logger.error("Cannot create ObjectName for remotemanager", e);
		}
		
		logger.debug("Workbench URI: "+serverUrl.getURLPath());

	}

	public static MBeanServerConnection getServerConnection(final long timeout) throws Exception {

		long                  waited = 0;
		MBeanServerConnection server = null;
		
		createServerUrl(21701);
		while(timeout>waited) {
			
			waited+=100;
			try {
				
				JMXConnector  conn = JMXConnectorFactory.connect(serverUrl);
				server             = conn.getMBeanServerConnection();
                if (server == null) throw new NullPointerException("MBeanServerConnection is null");
				break;
                
			} catch (Throwable ne) {
				if (waited>=timeout) {
					throw new Exception("Cannot get connection", ne);
				} else {
					if ("true".equals(System.getProperty("org.dawb.workbench.jmx.headless"))) {
						logger.error("Cannot find the MBeanServerConnection for the workbench client.\nThe headless property is set so workflow with continue without a client.");
						return null; 
					}
					Thread.sleep(100);
					continue;
				}
			}
		}
		return server;
	}

	private static int getFreePort(final int startPort) {
		
		int jmxServicePort = 21701;
		if (System.getProperty("com.isencia.jmx.service.port")!=null) {
			jmxServicePort = Integer.parseInt(System.getProperty("com.isencia.jmx.service.port"));
			logger.debug("Found 'com.isencia.jmx.service.port' set at port "+jmxServicePort);
			return jmxServicePort;
		}
		
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
