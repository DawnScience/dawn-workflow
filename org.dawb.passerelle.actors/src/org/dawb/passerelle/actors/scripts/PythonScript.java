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

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.python.PythonUtils;
import org.dawb.passerelle.common.actors.AbstractScriptTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.jython.ActorInterpreterUtils;
import org.dawnsci.python.rpc.PythonService;
import org.eclipse.core.resources.IResource;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.util.PythonInterpreter;
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
import uk.ac.diamond.scisoft.python.JythonInterpreterUtils;
import uk.ac.gda.util.list.ListUtils;

import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;

/**
 * Runs a script in Jython or Python.
 * 
 * @author gerring
 *
 */
public class PythonScript extends AbstractScriptTransformer {
	
	private static final Logger logger = LoggerFactory.getLogger(PythonScript.class);
	
	private static String[] INTERPRETER_CHOICES = new String[]{"Jython", "Python"};
	private static String[] PYLINK_CHOICES      = new String[]{"RPC"};
	private static String[] PYDEBUG_CHOICES     = new String[]{"Start new python rcp server",    "Python rpc server already running"};
	
	private final Parameter             createNewParameter;
	private boolean                     isNewInterpreter = false;
	
	private final Parameter             passInputsParameter;
	private boolean                     isPassInputs = true;
	
	private final StringParameter       interpreterTypeParam;

	private final StringParameter       outputsParam;
	private List<String>                outputs;

	private StringParameter             pythonCommand;
	private StringParameter             pythonLink;
	private StringParameter             pythonDebug;
	/**
	 * 
	 */
	private static final long serialVersionUID = 5076235276519285512L;

	@SuppressWarnings("serial")
	public PythonScript(CompositeEntity container, String name) throws Exception {
		super(container, name);
		
		createNewParameter = new Parameter(this,"Create Separate Interpreter",new BooleanToken(false));
		registerConfigurableParameter(createNewParameter);
		
		passInputsParameter = new Parameter(this,"Pass Inputs On",new BooleanToken(true));
		registerConfigurableParameter(passInputsParameter);

		interpreterTypeParam = new StringParameter(this,"Interpreter Type") {

			public String[] getChoices() {
				return INTERPRETER_CHOICES;
			}
		};
		interpreterTypeParam.setExpression(INTERPRETER_CHOICES[1]);
		registerConfigurableParameter(interpreterTypeParam);
		
		outputsParam = new StringParameter(this,"Dataset Outputs");
		registerConfigurableParameter(outputsParam);
		
		pythonCommand = new StringParameter(this, "Python Interpreter Command");
		registerConfigurableParameter(pythonCommand);
		pythonCommand.setExpression(PythonUtils.getPythonInterpreterCommand());
		
		// Expert param for changing between Jep and RPC
		pythonLink = new StringParameter(this,"Python Link") {

			public String[] getChoices() {
				return PYLINK_CHOICES;
			}
		};
		registerExpertParameter(pythonLink);
		pythonLink.setExpression(PYLINK_CHOICES[0]);
		
		// Expert param for turning on debug python server
		pythonDebug = new StringParameter(this,"Python Debug") {

			public String[] getChoices() {
				return PYDEBUG_CHOICES;
			}
		};
		registerExpertParameter(pythonDebug);
		pythonDebug.setExpression(PYDEBUG_CHOICES[0]);

    }


	@Override
	protected ResourceParameter getScriptParameter(Actor actor) throws Exception {
		
		return new ResourceParameter(actor, "Python Script", "Python Files", "*.py", "*.jy");
	}
	
	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == createNewParameter) {
		    isNewInterpreter = ((BooleanToken) createNewParameter.getToken()).booleanValue();
		} else if (attribute == passInputsParameter) {
			isPassInputs = ((BooleanToken) passInputsParameter.getToken()).booleanValue();
		} else if (attribute == outputsParam) {
			outputs = ListUtils.getList(outputsParam.getExpression());
		}
		super.attributeChanged(attribute);
		
	}

	/**
	 * Sets any data passed into the node as python datasets and then
	 * runs first the script and then returns the value of the expression.
	 */
	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws Exception {
				
		if (isPython()) {
			return getPythonRpcMessage(cache);
		} else {
			return getJythonMessage(cache);
		}
	}

	private DataMessageComponent getPythonRpcMessage(final List<DataMessageComponent> cache) throws Exception {

		PythonService service=null;
		try {
			if (PYDEBUG_CHOICES[1].equals(pythonDebug.getExpression())) {
				int port = PythonService.getDebugPort(); // Checks system property but is normally 8613
				service  = PythonService.openClient(port);
			} else {
				service = PythonService.openConnection(pythonCommand.getExpression());
			}
	
			final DataMessageComponent ret = isPassInputs ? MessageUtils.mergeAll(cache) : new DataMessageComponent();
			ret.setMeta(MessageUtils.getMeta(cache));
			ret.putScalar("python_script", getResource().getName());
			
			final Map<String,Object>  data = ret.getList()!=null
					                       ? new HashMap<String,Object>(ret.getList())
					                       : new HashMap<String,Object>();   
					                    		   
					                    
			if (ret.getScalar()!=null) {
				for (String name : ret.getScalar().keySet()) {
					
					if (name.indexOf('.')>-1) continue;
					final String value = ret.getScalar().get(name);
					
					// The name must be legal one:
					if (!PythonUtils.isLegalName(name)) {
						name = PythonUtils.getLegalVarName(name, ret.getList().keySet());
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
			if (outputs==null) outputs=Collections.emptyList();
			final List<String> toRead = new ArrayList<String>(outputs.size()+data.size());
			toRead.addAll(outputs);
			toRead.addAll(data.keySet());
			final Map<String,? extends Object> result;
			try {
				result = service.runScript(file.getLocation().toOSString(), data, toRead);
			} catch (AnalysisRpcException e) {
				if (e.getCause() instanceof AnalysisRpcRemoteException) {
					AnalysisRpcRemoteException remoteException = (AnalysisRpcRemoteException) e.getCause();
					String formatException = service.formatException(remoteException);
					throw createDataMessageException(formatException, e);
				} else {
					throw e;
				}
			}
            for (String varName : result.keySet()) {
				final Object val = result.get(varName);
				if (val instanceof AbstractDataset) {
					final AbstractDataset set = (AbstractDataset)val;
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
			if (service!=null) service.stop();
		}


	}


	private DataMessageComponent getJythonMessage(final List<DataMessageComponent> cache) throws Exception {
		
		PythonInterpreter interpreter;
		if (isNewInterpreter) { // (Takes a while)
			interpreter = JythonInterpreterUtils.getInterpreter();
		} else {
			interpreter = ActorInterpreterUtils.getInterpreterForRun(this);
		}

		// We save the names of the abstract data sets passed in
		// these are read back and passed on sometimes.
		List<String> inputs = new ArrayList<String>(7);
		
		for (DataMessageComponent dataMessageComponent : cache) {
			if (dataMessageComponent.getList()==null) continue;
			for (String name : dataMessageComponent.getList().keySet()) {
				
				final Object ob = dataMessageComponent.getList().get(name);
				interpreter.set(name, ob);
				if (ob instanceof AbstractDataset) {
				    interpreter.exec(name+" = dnp.Sciwrap("+name+")");
					if (isPassInputs) {
						inputs.add(name);
					}
				}
			}
		}
		
		final IResource file = getResource();
		if (file.exists()) interpreter.execfile(new FileInputStream(file.getLocation().toOSString()));
				
		final DataMessageComponent ret = new DataMessageComponent();		
		final Map<String,Serializable>  data = new HashMap<String, Serializable>(7);
		if (outputs==null) outputs=Collections.emptyList();
		final List<String>         out = new ArrayList<String>(outputs.size()+inputs.size());
		out.addAll(outputs);
		out.addAll(inputs);
		
		for (String name : out) {
			PyObject ob = interpreter.get(name);
			
			AbstractDataset ds = (AbstractDataset)ob.__tojava__(AbstractDataset.class);
			if (ds instanceof PyProxy) ds = ds.getView();
			
			ds.setName(name);
			data.put(name, ds);
		}
		ret.setList(data);

		ret.setMeta(MessageUtils.getMeta(cache));
		ret.addScalar(MessageUtils.getScalar(cache));
		ret.putScalar("Jython Script", getResource().getName());
		
		return ret;
	}

	private boolean isPython() {
		final String value = interpreterTypeParam.getExpression();
		return INTERPRETER_CHOICES[1].equals(value);
	}

	@Override
	public String getExtendedInfo() {
		return "Parses an expression of data sets using a Jython expression or script file.";
	}
	
	@Override
	public void setMomlResource(IResource momlFile) {
		// Do nothing
	}

	@Override
	protected String getOperationName() {
		return isPython()?"python":"jython";
	}

	@Override
	protected String createScript() {
		
		final StringBuilder buf = new StringBuilder();
		
		if (isPython()) {
			buf.append("import numpy as np\n");
		} else {
			buf.append("import scisoftpy as dnp\n");
			buf.append("\n\n");

			final List<IVariable> vars = getInputVariables();
			if (vars!=null&&vars.size()>0) {
				buf.append("# Jython script for manipulating datasets.\n");
				buf.append("# The variables going into this actor in the workflow are available in the script.\n");
				buf.append("# Expected variable sets available and set when this script is run:\n");
				for (IVariable iVariable : vars) {
					if (iVariable.getVariableType()==VARIABLE_TYPE.ARRAY) {
						buf.append("# ");
						buf.append(iVariable.getVariableName());
						buf.append("\n");
					}
				}
				buf.append("\n# Please provide maths of these variables and other available ones in the following lines.\n");
			}
		}

		return buf.toString();
	}

    /**
     * Adds the scalar and the 
     */
	@Override
	public List<IVariable> getOutputVariables() {
		
        final List<IVariable> ret = super.getOutputVariables();
     
        if (outputs==null) outputs = ListUtils.getList(outputsParam.getExpression());
        if (outputs!=null) for (String name : outputs) { // Might not be an array.
        	ret.add(new Variable(name, VARIABLE_TYPE.ARRAY, "[...]"));
        }
		
		if (isPassInputs) {
		    List<IVariable> up = getInputVariables();
		    for (IVariable iVariable : up) {
				if (iVariable.getVariableType()==VARIABLE_TYPE.ARRAY) {
					ret.add(iVariable);
				}
			}
		}
		
        return ret;
	}

}
