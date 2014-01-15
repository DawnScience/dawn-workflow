/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawb.passerelle.actors.scripts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dawb.common.python.PyDevUtils;
import org.dawb.common.python.PyDevUtils.AvailableInterpreter;
import org.dawb.common.python.PythonUtils;
import org.dawb.common.util.list.ListUtils;
import org.dawb.passerelle.common.actors.AbstractScriptTransformer;
import org.dawb.passerelle.common.actors.IDescriptionProvider.Requirement;
import org.dawb.passerelle.common.actors.IDescriptionProvider.VariableHandling;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawnsci.python.rpc.AnalysisRpcPythonPyDevService;
import org.dawnsci.python.rpc.PythonRunScriptService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.rpc.AnalysisRpcException;
import uk.ac.diamond.scisoft.analysis.rpc.AnalysisRpcRemoteException;

import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;

/**
 * Runs a script in Python using PyDev.
 * 
 */
public class PythonPydevScript extends AbstractScriptTransformer {
	private static final long serialVersionUID = 7006402070293405245L;
	private static final Logger logger = LoggerFactory
			.getLogger(PythonScript.class);

	/** The possible interpreter to use */
	private final StringParameter interpreterParam;

	private final Parameter passInputsParameter;
	private boolean isPassInputs = true;
	private final StringParameter outputsParam;
	private List<String> outputs;
	private final Parameter createNewParameter;
	private boolean isNewInterpreter = false;
	private final Parameter debugParameter;
	private boolean isDebug = false;

	public PythonPydevScript(CompositeEntity container, String name)
			throws Exception {
		
		super(container, name);
		setDescription(scriptFileParam, Requirement.ESSENTIAL, VariableHandling.EXPAND, "The path to the script to run. The script must contain a run(..) method which takes the workflow variables required along with **kwargs and returns a dictionary of output values to pass on to the rest of the workflow.");

		interpreterParam = new StringParameter(this, "Interpreter") {
			private static final long serialVersionUID = -1140080995799869524L;

			public String[] getChoices() {
				return PyDevUtils.getChoices(false,
						PythonPydevScript.this.getProject());
			}
		};

		interpreterParam.setExpression(PyDevUtils.getChoices(true,
				this.getProject())[0]);
		registerConfigurableParameter(interpreterParam);
		setDescription(interpreterParam, Requirement.ESSENTIAL, VariableHandling.NONE, "The python pydev interpreter to use.");

		passInputsParameter = new Parameter(this, "Pass Inputs On",
				new BooleanToken(true));
		registerConfigurableParameter(passInputsParameter);
		setDescription(passInputsParameter, Requirement.OPTIONAL, VariableHandling.NONE, "All inputs may be added to the message to pass on. Where inputs clash with outputs of the script, the outputs override.");

		outputsParam = new StringParameter(this, "Dataset Outputs");
		registerConfigurableParameter(outputsParam);
		setDescription(outputsParam, Requirement.OPTIONAL, VariableHandling.NONE, "Please declare the names of the variables contained in the dictionary returned from the run method that should enter the workflow here.");

		createNewParameter = new Parameter(this, "Create Separate Interpreter", new BooleanToken(true));
		registerConfigurableParameter(createNewParameter);
		setDescription(createNewParameter, Requirement.OPTIONAL, VariableHandling.NONE, "Used to set if a separate interpreter should be used for running each pipeline slug.");

		debugParameter = new Parameter(
				this,
				"Run Script in Debug Mode (requires running PyDev Debug server)",
				new BooleanToken(false));
		registerConfigurableParameter(debugParameter);
		setDescription(debugParameter, Requirement.OPTIONAL, VariableHandling.NONE, "Click on if you want to run the workflow in debug mode. You will also need to start the python debug server in the 'Debug' perpsective.");


		setDescription("Run a python script using the pydev configuration for the project. You can also use pydev debug when the workflow is run. The python script must contain a run(...) method. The variables of each required workflow variable must be declared as arguments and terminated with '**kwargs'.");
	}

	@Override
	protected ResourceParameter getScriptParameter(Actor actor)
			throws Exception {

		return new ResourceParameter(actor, "Python Script", "Python Files",
				"*.py");
	}

	/**
	 * @param attribute
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == createNewParameter) {
			isNewInterpreter = ((BooleanToken) createNewParameter.getToken())
					.booleanValue();
		} else if (attribute == debugParameter) {
			isDebug = ((BooleanToken) debugParameter.getToken()).booleanValue();
		} else if (attribute == passInputsParameter) {
			isPassInputs = ((BooleanToken) passInputsParameter.getToken())
					.booleanValue();
		} else if (attribute == outputsParam) {
			outputs = ListUtils.getList(outputsParam.getExpression());
		}
		super.attributeChanged(attribute);
	}

	/**
	 * Sets any data passed into the node as python datasets and then runs first
	 * the script and then returns the value of the expression.
	 */
	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws Exception {

		AnalysisRpcPythonPyDevService stopService = null;
		try {
			AvailableInterpreter info = PyDevUtils.getMatchingChoice(
					interpreterParam.getExpression(), this.getProject());
			if (info == null || info.info == null) {
				String availableChoices = StringUtils.join(
						PyDevUtils.getChoices(false, this.getProject()), ", ");
				final String msg = "Selected interpreter does not exist in current PyDev configuration: "
						+ interpreterParam.getExpression()
						+ " available configurations: " + availableChoices;
				logger.error(msg);
				throw new Exception(msg);
			}

			final PythonRunScriptService pythonRunScriptService;
			if (isNewInterpreter) {
				stopService = new AnalysisRpcPythonPyDevService(info.info,
						getProject());
				pythonRunScriptService = new PythonRunScriptService(
						stopService, isDebug);
			} else {
				File containingDirectory = getResource().getLocation().toFile().getParentFile();
				pythonRunScriptService = getService(isDebug, getProject(),
						info.info, containingDirectory);
			}

			@SuppressWarnings("unchecked")
			DataMessageComponent mergeCache = MessageUtils.mergeAll(cache);
			final DataMessageComponent ret;
			if (isPassInputs)
				ret = mergeCache;
			else
				ret = new DataMessageComponent();
			ret.setMeta(MessageUtils.getMeta(cache));
			ret.putScalar("python_script", getResource().getName());

			final Map<String, Object> data;
			if (ret.getList() != null)
				data = new HashMap<String, Object>(ret.getList());
			else
				data = new HashMap<String, Object>();

			if (ret.getScalar() != null) {
				for (String name : ret.getScalar().keySet()) {

					if (name.indexOf('.') > -1)
						continue;
					final String value = ret.getScalar().get(name);

					// The name must be legal one:
					if (!PythonUtils.isLegalName(name)) {
						name = PythonUtils.getLegalVarName(name, ret.getList()
								.keySet());
					}
					try {
						final int ival = Integer.parseInt(value);
						data.put(name, ival);

					} catch (Throwable t) {
						try {
							final double dval = Double.parseDouble(value);
							data.put(name, dval);

						} catch (Throwable t2) {
							data.put(name, value);
						}
					}
				}
			}

			final IResource file = getResource();
			if (outputs == null)
				outputs = Collections.emptyList();
			final List<String> toRead = new ArrayList<String>(outputs.size()
					+ data.size());
			toRead.addAll(outputs);
			toRead.addAll(data.keySet());

			final Map<String, ? extends Object> result;
			try {
				result = pythonRunScriptService
						.runScript(file.getLocation().toOSString(), data);
			} catch (AnalysisRpcException e) {
				if (e.getCause() instanceof AnalysisRpcRemoteException) {
					AnalysisRpcRemoteException remoteException = (AnalysisRpcRemoteException) e.getCause();
					String formatException = pythonRunScriptService.formatException(remoteException);
					throw createDataMessageException(formatException, e);
				} else {
					throw e;
				}
			}
			for (String varName : result.keySet()) {
				final Object val = result.get(varName);
				if (val instanceof AbstractDataset) {
					final AbstractDataset set = (AbstractDataset) val;
					set.setName(varName);
					ret.addList(varName, set);
				} else {
					ret.putScalar(varName, String.valueOf(val));
				}
			}

			return ret;

		} catch (DataMessageException e) {
			throw e;
		} catch (Throwable e) {
			throw createDataMessageException(e.getMessage(), e);
		} finally {
			if (stopService != null)
				stopService.stop();
		}
	}

	private static List<Tuple<Tuple3<IProject, IInterpreterInfo, File>, AnalysisRpcPythonPyDevService>> services = Collections
			.synchronizedList(new ArrayList<Tuple<Tuple3<IProject, IInterpreterInfo, File>, AnalysisRpcPythonPyDevService>>());

	/**
	 * Get a PythonRunScriptService, launching an AnalysisRpcPythonPyDevService if no suitable
	 * ones exist already in the {@link #services} cache.
	 * <p>
	 * To be suitable, a Python process must refer to the same IProject, IInterpreterInfo and
	 * the same source directory for the run script.
	 */
	private static PythonRunScriptService getService(boolean isDebug,
			final IProject project, final IInterpreterInfo info, final File containingDirectory)
			throws IOException, AnalysisRpcException {
		synchronized (services) {
			Tuple3<IProject, IInterpreterInfo, File> tuple = new Tuple3<IProject, IInterpreterInfo, File>(
					project, info, containingDirectory);
			for (Tuple<Tuple3<IProject, IInterpreterInfo, File>, AnalysisRpcPythonPyDevService> service : services) {
				if (service.o1.equals(tuple)) {
					return new PythonRunScriptService(service.o2, isDebug, true);
				}
			}
			AnalysisRpcPythonPyDevService rpcservice = new AnalysisRpcPythonPyDevService(
					info, project);
			services.add(new Tuple<Tuple3<IProject, IInterpreterInfo, File>, AnalysisRpcPythonPyDevService>(
					tuple, rpcservice));
			return new PythonRunScriptService(rpcservice, isDebug);
		}
	}

	@Override
	public String getExtendedInfo() {
		return "Parses an expression of data sets using a Python script file, with python configured by PyDev.";
	}

	@Override
	public void setMomlResource(IResource momlFile) {
		// Do nothing
	}

	@Override
	protected String getOperationName() {
		return "python";
	}

	@Override
	protected String createScript() {
		return "import numpy as np\n";
	}

	@Override
	public List<IVariable> getOutputVariables() {

		final List<IVariable> ret = super.getOutputVariables();

		if (outputs == null)
			outputs = ListUtils.getList(outputsParam.getExpression());
		if (outputs != null)
			for (String name : outputs) { // Might not be an array.
				ret.add(new Variable(name, VARIABLE_TYPE.ARRAY, "[...]"));
			}

		if (isPassInputs) {
			List<IVariable> up = getInputVariables();
			for (IVariable iVariable : up) {
				if (iVariable.getVariableType() == VARIABLE_TYPE.ARRAY) {
					ret.add(iVariable);
				}
			}
		}

		return ret;
	}
}
