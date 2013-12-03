/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.process;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.actors.AbstractPassModeTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;
import com.isencia.util.commandline.ManagedCommandline;

/**
 * At first site this might not appear like an AbstractDataMessageTransformer but
 * actually several inputs like replaces may be passed to the actor.
 * 
 * @author gerring
 *
 */
public class ProcessTransformer extends AbstractDataMessageTransformer {

	private static final Logger logger = LoggerFactory.getLogger(ProcessTransformer.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -6156476506138933499L;
	
	private final StringParameter cmdParam;
	private final StringParameter outputVar;
	private final StringParameter winCmdParam;
	private final Parameter       waitParam;
	private final StringParameter dirParam;
	private final DateFormat      dateFormat;

	public ProcessTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		this.dateFormat = new SimpleDateFormat("dd_MMM_yyyy[HH'h'_mm'm'_ss's'_SS'ms']");
		
		cmdParam = new StringParameter(this, "Command");
		registerConfigurableParameter(cmdParam);
		
		winCmdParam = new StringParameter(this, "Windows Command");
		registerConfigurableParameter(winCmdParam);
		
	    dirParam = new StringParameter(this, "Run Directory");
		registerConfigurableParameter(dirParam);
		//dirParam.setExpression("${project_name}/processes/${execution_date}");
		
		waitParam = new Parameter(this,"Wait",new BooleanToken(true));
		registerConfigurableParameter(waitParam);
		
	    outputVar = new StringParameter(this, "Output Variable");
		registerConfigurableParameter(outputVar);


		// Control parent parameters
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
     
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {

		try {
			
			final String                 cmd;
			if (isWindowsOS() && winCmdParam.getExpression()!=null && !"".equals(winCmdParam.getExpression())) {
				cmd = ParameterUtils.getSubstituedValue(winCmdParam, cache);
			} else {
				cmd = ParameterUtils.getSubstituedValue(cmdParam, cache);
			}
			
			final ManagedCommandline command = new ManagedCommandline();
			if (isLinuxOS()) {
				command.addArguments(new String[]{"/bin/sh", "-c", cmd});
			} else {
				command.addArguments(new String[]{"cmd.exe", "/C", cmd});
			}
			
			
			final String outputExpressionName = ParameterUtils.getSubstituedValue(outputVar, cache);
			if (outputExpressionName!=null) {
				command.setStreamLogsToLoggingAndSaved(true);
			} else {
                command.setStreamLogsToLogging(true);
			}
            command.setEnv(System.getenv());
            
			final long time = System.currentTimeMillis();
			
			final DataMessageComponent ret = MessageUtils.copy(cache);
			ret.putScalar("execution_time", ""+time);
			ret.putScalar("execution_date", dateFormat.format(time));

			if (dirParam.getExpression()!=null && !"".equals(dirParam.getExpression())) {
				
				final String   dir       = ParameterUtils.getSubstituedValue(dirParam, ret);
	            final String   workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				final File     runDir    = new File(workspace+"/"+dir);
				if (!runDir.exists()) {
					runDir.mkdirs();
					AbstractPassModeTransformer.refreshResource(ModelUtils.getProject(this));
				}
				
	            command.setWorkingDir(runDir);
			}
            
			final Process            process = command.execute();
			
			if (((BooleanToken)waitParam.getToken()).booleanValue()) {
				final int retCode = process.waitFor();
				logger.debug("Process '"+cmd+"' exited with code "+retCode);
			}
			
			AbstractPassModeTransformer.refreshResource(ModelUtils.getProject(this));
			
			if (outputExpressionName!=null) {
				ret.putScalar(outputExpressionName, command.getStdoutAsString()!=null ? command.getStdoutAsString().trim() : "");
			}
			
			return ret; 
			
		} catch (Exception ne) {
			throw createDataMessageException("Cannot run command "+cmdParam.getExpression(), ne);
		}
		
	}
	/**
	 * @return true if windows
	 */
	static public boolean isWindowsOS() {
		return (System.getProperty("os.name").indexOf("Windows") == 0);
	}
	/**
	 * @return true if linux
	 */
	public static boolean isLinuxOS() {
		String os = System.getProperty("os.name");
		return os != null && os.startsWith("Linux");
	}
    /**
     * Adds the scalar and the 
     */
	@Override
	public List<IVariable> getOutputVariables() {
		
        final List<IVariable> ret = super.getOutputVariables();
		final long time = System.currentTimeMillis();
		ret.add(new Variable("execution_time", VARIABLE_TYPE.SCALAR, time));
		ret.add(new Variable("execution_date", VARIABLE_TYPE.SCALAR, dateFormat.format(time)));
		
		try {
			final String outputExpressionName = ParameterUtils.getSubstituedValue(outputVar);
			if (outputExpressionName!=null) ret.add(new Variable(outputExpressionName, VARIABLE_TYPE.SCALAR, "stdout"));

		} catch (Exception e) {
			logger.error("Cannot get expression name for "+outputVar.getDisplayName());
		}

        return ret;
	}


	@Override
	protected String getExtendedInfo() {
		return "An actor which runs a native process, may have input and output files.";
	}

	@Override
	protected String getOperationName() {
		return "System Command";
	}

}
