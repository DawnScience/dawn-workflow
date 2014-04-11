/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.file;

import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.dawb.common.util.io.Grep;
import org.dawb.passerelle.common.actors.AbstractDataMessageSink;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawb.passerelle.common.utils.SubstituteUtils;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;

/**
 * Attempts to plot data in eclipse or writes a csv file with the data if 
 * that is not possible.
 * 
 * @author gerring
 *
 */
public class MonitorFolderSink extends AbstractDataMessageSink {
	
	private static final Logger logger = LoggerFactory.getLogger(MonitorFolderSink.class);
	
	private Parameter dirNameParam;
	private Parameter monitorParam;
	private String    dirName;
	private String    fullPath;
	private boolean   isMonitoring;

	public MonitorFolderSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		
		dirNameParam = new StringParameter(this, "Directory Path");
		dirNameParam.setExpression("${file_dir}"); // As this is often the variable
		registerConfigurableParameter(dirNameParam);
		
		monitorParam = new Parameter(this, "Start Monitor");
		monitorParam.setToken(new BooleanToken(false));
		registerConfigurableParameter(monitorParam);
		
		memoryManagementParam.setVisibility(Settable.NONE);

	}
	
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == dirNameParam) {
			dirName = dirNameParam.getExpression();
		} else
			super.attributeChanged(attribute);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8241926002035209866L;

	@Override
	protected void sendCachedData(final List<DataMessageComponent> cache) throws ProcessingException {


		final List<String> vars = Grep.group(dirName, ParameterUtils.VARIABLE_EXPRESSION, 1);

		final DataMessageComponent comp = MessageUtils.mergeAll(cache);

		try {
			final Map<String,String> values = MessageUtils.getValues(comp, vars, this);
			if (!vars.isEmpty() && values.isEmpty()) return;

			this.fullPath     = SubstituteUtils.substitute(dirName, values);
			try {
				final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(fullPath);
				if (res!=null) fullPath = res.getLocation().toOSString();
			} catch (Exception ne) {
				logger.error("Cannot find relative path! "+fullPath, ne);
			}
            this.isMonitoring = ((BooleanToken)monitorParam.getToken()).booleanValue();
			fireOpenMonitor();
		
		} catch (Exception iae) {
			logger.error("Cannot open file "+dirName+". Expand probably failed on value because scalar value not present.");
		}

	}
	
	private void fireOpenMonitor() {
		try {
			final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
			final Object ob = client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "monitorDirectory", new Object[]{fullPath,isMonitoring}, new String[]{String.class.getName(), boolean.class.getName()});
			if (ob==null || !((Boolean)ob).booleanValue()) {
				logger.error("Cannot monitor directory "+fullPath);
			}
		} catch (Exception e) {
			logger.debug("Cannot get workbench service, however this is legal for when there is no workbench and the workflow runs in batch.");
			logger.trace("Cannot get workbench service, however this is legal for when there is no workbench and the workflow runs in batch.",e);
		}		
	}

	protected void doWrapUp() throws TerminationException {
		super.doWrapUp();
		if (fullPath==null) return;
		
		/**
		 * We refresh the directory again, now that the images are no longer being written.
		 */
		if (isFinishRequested()) fireOpenMonitor();
	}

}
