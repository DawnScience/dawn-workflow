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
import org.dawb.common.python.rpc.AnalysisRpcPythonPyDevService;
import org.dawb.common.python.rpc.PythonRunScriptService;
import org.dawb.passerelle.common.actors.AbstractScriptTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.Tuple;
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
import uk.ac.gda.util.list.ListUtils;

import com.isencia.passerelle.actor.Actor;
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

		passInputsParameter = new Parameter(this, "Pass Inputs On",
				new BooleanToken(true));
		registerConfigurableParameter(passInputsParameter);
		outputsParam = new StringParameter(this, "Dataset Outputs");
		registerConfigurableParameter(outputsParam);

		createNewParameter = new Parameter(this, "Create Separate Interpreter",
				new BooleanToken(false));
		registerConfigurableParameter(createNewParameter);

		debugParameter = new Parameter(
				this,
				"Run Script in Debug Mode (requires running PyDev Debug server)",
				new BooleanToken(false));
		registerConfigurableParameter(debugParameter);

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
				pythonRunScriptService = getService(isDebug, getProject(),
						info.info);
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

			final Map<String, ? extends Object> result = pythonRunScriptService
					.runScript(file.getLocation().toOSString(), data);
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

		} catch (Throwable e) {
			// Cut off all stack traces above and including "result = vars[funcName](**inputs)" since
			// they are generated by DAWN python code (DAWNSCI-736)
			String stringToMatch = "    result = vars[funcName](**inputs)";
			String message = e.getMessage();
			int startIndex = message.indexOf(stringToMatch);
			int endIndex = startIndex + stringToMatch.length();
			String shortMessage = message.substring(endIndex);
			throw createDataMessageException(shortMessage, e);
		} finally {
			if (stopService != null)
				stopService.stop();
		}
	}

	private static List<Tuple<Tuple<IProject, IInterpreterInfo>, AnalysisRpcPythonPyDevService>> services = Collections
			.synchronizedList(new ArrayList<Tuple<Tuple<IProject, IInterpreterInfo>, AnalysisRpcPythonPyDevService>>());

	private static PythonRunScriptService getService(boolean isDebug,
			final IProject project, final IInterpreterInfo info)
			throws IOException, AnalysisRpcException {
		synchronized (services) {
			Tuple<IProject, IInterpreterInfo> tuple = new Tuple<IProject, IInterpreterInfo>(
					project, info);
			for (Tuple<Tuple<IProject, IInterpreterInfo>, AnalysisRpcPythonPyDevService> service : services) {
				if (service.o1.equals(tuple)) {
					return new PythonRunScriptService(service.o2, isDebug, true);
				}
			}
			AnalysisRpcPythonPyDevService rpcservice = new AnalysisRpcPythonPyDevService(
					info, project);
			services.add(new Tuple<Tuple<IProject, IInterpreterInfo>, AnalysisRpcPythonPyDevService>(
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
