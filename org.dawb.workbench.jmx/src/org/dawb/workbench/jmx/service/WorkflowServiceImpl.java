/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;

import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Not public the details of the service are hidden to the rest of the world.
 * @author gerring
 * 
 * Example Command to start workbench which we enumulate here:
 * 
 * ./dawn -noSplash -application com.isencia.passerelle.workbench.model.launch 
 * -data $WORKSPACE -consolelog -os linux -ws gtk -arch $HOSTTYPE -vmargs -Dmodel=$MODEL
 *
 */
class WorkflowServiceImpl implements IWorkflowService {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

	private Process       workflow;
	private PrintWriter   outw, errw;
	private StreamGobbler out,err;
	private IRemoteServiceProvider prov;
	private boolean       logDirectly=true;
	private RemoteWorkbenchAgent agent;

	public WorkflowServiceImpl(IRemoteServiceProvider prov) {
		this.prov = prov;
		this.outw = null;
		this.errw = null;
	}

	@Override
	public int run() throws Exception {

		final Process process = start();
		process.waitFor();
		
		return process.exitValue();
	}

	@Override
	public void setLoggingStreams(PrintWriter out, PrintWriter err) {
		this.outw = out;
		this.errw = err;
	}

	@Override
	public Process start() throws Exception {
		
		// Start an agent which the workflow can rely on to do user
		// interface and other actions.
		if (prov.getRemoteWorkbench()!=null) { // If null there is no workbench or the default one should be used.
			this.agent = new RemoteWorkbenchAgent(prov, true);
			agent.start();
		}
		
		final String line = createExecutionLine(prov);
		logger.debug("Execution line: "+line);
		final String[]  command;
		if (isLinuxOS()) {
			command = new String[]{"/bin/sh", "-c", line};
		} else {
			command = new String[]{"cmd", "/C", line};
		}
		 
		final Map<String,String> env = System.getenv();
		this.workflow     = Runtime.getRuntime().exec(command, getStringArray(env));
		this.out          = new StreamGobbler(workflow.getInputStream(), outw, "workflow output");
		out.setStreamLogsToLogging(logDirectly);
		out.start();
		
		this.err      = new StreamGobbler(workflow.getErrorStream(), errw, "workflow error");
		err.setStreamLogsToLogging(logDirectly);
		err.start();
		
		return workflow; // We only support 1 at the moment
	}

	private String[] getStringArray(Map<String, String> env) {
		
		final String[] ret = new String[env.size()];
		int i = 0;
		for (String key : env.keySet()) {
			ret[i] = key+"="+env.get(key);
			++i;
		}
		return ret;
	}

	/*
	 * Example:
	 * 
	 * C:\Users\fcp94556\Desktop\DawnMaster/dawn.exe -noSplash 
	 * -application com.isencia.passerelle.workbench.model.launch 
	 * -data C:/Work/runtime-uk.ac.diamond.dawn.product -consolelog -properties <a file path> -vmargs 
	 * -Dorg.dawb.workbench.jmx.headless=true -Dcom.isencia.jmx.service.terminate=true 
	 * -Dmodel=C:/Work/runtime-uk.ac.diamond.dawn.product/trace/trace_workflow.moml
	 */
	private String createExecutionLine(IRemoteServiceProvider prov) throws IOException {
		
		final StringBuilder buf = new StringBuilder();
		
		// Get the path to the workspace and the model path
		final String install   = prov.getInstallationPath();
		String workspace = prov.getWorkspacePath();
		if (workspace.contains(" ")) workspace = "\""+workspace+"\"";
		
		final String model     = prov.getModelPath();
		final int port         = agent!=null ? agent.getCurrentPort() : -1;
		final boolean isServiceTerminate  = prov.getServiceTerminate();
		final boolean isTangoSpecMockMode = prov.getTangoSpecMockMode();
		
		buf.append(install);
		buf.append(" -noSplash -application com.isencia.passerelle.workbench.model.launch ");
		buf.append(" -data ");
		buf.append(workspace);
		
		if (prov.getProperties()!=null) {
			buf.append(" -properties ");
			final File properties = storeProperties(prov.getProperties());
			String value = properties.getAbsolutePath();
            if (value.contains(" ")) value = "\""+value+"\"";
            buf.append(value);
		}

		buf.append(" -consoleLog -vmargs ");
		buf.append(" -Dmodel=");
		buf.append(model);
		buf.append(" -Dcom.isencia.jmx.service.workspace=");
		buf.append(workspace);
		if (port>0) {
			buf.append(" -Dcom.isencia.jmx.service.port=");
			buf.append(port);
		}
		if (isServiceTerminate) buf.append(" -Dcom.isencia.jmx.service.terminate=true");
		if (isTangoSpecMockMode) 
			buf.append(" -Dorg.dawb.test.session=true");
		else
			buf.append(" -Dorg.dawb.test.session=false");
		
		if (prov.getProperties()!=null) {

			if (prov.getProperties().containsKey("logLocation")) {
				// Two spaces deals with the value of the last property being \
				buf.append("  > "+prov.getProperties().get("logLocation"));
			}

		}
		return buf.toString();
	}

	private final static File storeProperties(final Properties props) throws IOException {

		final File ret = File.createTempFile("workflow_", ".properties");
		ret.deleteOnExit();
		
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(ret));
		try {			
			props.store(out, "DAWN Workflow properties file");
			return ret;
		} finally {
			out.close();
		}
	}

	/**
	 * This attempts to call a nice stop on the workflow. The clean() method should
	 * still be called afterwards. You can wait after calling stop but normally stop
	 * blocks until the workflow cycle in Ptolemy 2 is broken. If there are deamon threads,
	 * the VM may not exit normally, however the clear() method will attempt to 
	 * stop it anyway.
	 */
	@Override
	public void stop(long id) throws Exception {
		if (this.agent != null) {
			try {
			    final MBeanServerConnection conn = RemoteWorkflow.getServerConnection(1000);
			    conn.invoke(RemoteWorkflow.REMOTE_MANAGER, "stop", null, null);
	
			} catch (Exception ne) {
				throw new Exception("Cannot call workflow service for clean stop!", ne);
			}
		}
	}
	
	/**
	 * Returns the current output from the workflow.
	 */
	public String getStandardOut() {
		if (out==null) return null;
		return out.getStreamDataAsString();
	}
	
	/**
	 * Returns the current error from the workflow.
	 */
	public String getStandardError() {
		if (err==null) return null;
		return err.getStreamDataAsString();
	}

	@Override
	public boolean isActive(long id) throws Exception {
		if (out==null) return false;
		return !out.isClosed();
	}

	@Override
	public void clear() throws Exception {
		
		if (this.agent != null) {
			try {
				agent.stop();
			} catch (javax.management.InstanceNotFoundException ex) {
				logger.trace("Service could not be stopped",ex);
			}
		}
			
		if (workflow!=null) {
			workflow.destroy();
			logger.info("Workflow execution aborted.");
		}
		workflow = null;
		out = null;
		err = null;
	}

	@Override
	public void setLogToStandardOut(boolean b) {
		logDirectly = b;
	}
	
	/**
	 * @return true if linux
	 */
	public static boolean isLinuxOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("linux");
	}

	@Override
	public void setProperty(String key, String value) {
		System.setProperty(key, value);
	}
}
