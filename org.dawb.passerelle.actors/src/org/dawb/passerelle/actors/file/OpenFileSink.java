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
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Attempts to plot data in eclipse or writes a csv file with the data if 
 * that is not possible.
 * 
 * @author gerring
 *
 */
public class OpenFileSink extends AbstractDataMessageSink {
	
	private static final Logger logger = LoggerFactory.getLogger(OpenFileSink.class);
	
	private Parameter fileNameParam;
	private String    fileName;

	public OpenFileSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		
		fileNameParam = new StringParameter(this, "File Path");
		fileNameParam.setExpression("${file_path}"); // As this is often the variable
		registerConfigurableParameter(fileNameParam);
		
		memoryManagementParam.setVisibility(Settable.NONE);

	}
	
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == fileNameParam) {
			fileName = fileNameParam.getExpression();
		} else
			super.attributeChanged(attribute);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8241926002035209866L;

	@Override
	protected void sendCachedData(final List<DataMessageComponent> data) throws ProcessingException {


		final List<String> vars = Grep.group(fileName, ParameterUtils.VARIABLE_EXPRESSION, 1);

		for (DataMessageComponent comp : data) {
			
			try {
				final Map<String,String> values = MessageUtils.getValues(comp, vars, this);
				if (!vars.isEmpty() && values.isEmpty()) continue;
				
				final String fullPaths          = SubstituteUtils.substitute(fileName, values);
	
				final String[] paths = fullPaths.split("\\n");
				for (String fullPath : paths) {
					try {
						final MBeanServerConnection client = ActorUtils.getWorkbenchConnection();
						final Object ob = client.invoke(RemoteWorkbenchAgent.REMOTE_WORKBENCH, "openFile", new Object[]{fullPath}, new String[]{String.class.getName()});
						if (ob==null || !((Boolean)ob).booleanValue()) {
							logger.error("Cannot open file "+fullPath);
						}
					} catch (Exception e) {
						logger.debug("Cannot get workbench service, however this is legal for when there is no workbench and the workflow runs in batch.");
						logger.trace("Cannot get workbench service, however this is legal for when there is no workbench and the workflow runs in batch.",e);
						continue;
					}
				}
			} catch (Exception iae) {
				logger.error("Cannot open file "+fileName+". Expand probably failed on value because scalar value not present.");
				continue;
			}
		}

	}

}
