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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jep.Jep;
import jep.JepException;

import org.dawb.gda.extensions.jython.JythonInterpreterUtils;
import org.dawb.common.python.NumpyUtils;
import org.dawb.common.python.PythonUtils;
import org.dawb.common.python.rpc.PythonService;
import org.dawb.passerelle.common.actors.AbstractScriptTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.jython.ActorInterpreterUtils;
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
import uk.ac.gda.util.list.ListUtils;

import com.isencia.passerelle.actor.Actor;
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
	private static String[] PYLINK_CHOICES      = new String[]{"RPC",    "Jep"};
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
			if (PYLINK_CHOICES[0].equals(pythonLink.getExpression())) {
				// Normally RPC
			    return getPythonRpcMessage(cache);
			} else {
				return getPythonJepMessage(cache);
			}
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
	
			final DataMessageComponent ret = MessageUtils.mergeAll(cache);
			ret.setMeta(MessageUtils.getMeta(cache));
			ret.putScalar("python_script", getResource().getName());
			
			final Map<String,Object>  data = ret.getList()!=null
					                       ? new HashMap<String,Object>(ret.getList())
					                       : new HashMap<String,Object>();   
					                    		   
					                    
			if (ret.getScalar()!=null) {
				for (String name : ret.getScalar().keySet()) {
					
					if (name.indexOf('.')>-1) continue;
					final String value = ret.getScalar().get(name);
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
			
			// We process the names in data to ensure that all the variables are 
			// legal python syntax
			for (Iterator<String> it = data.keySet().iterator(); it.hasNext();) {
				final String name = it.next();
				if (!PythonUtils.isLegalName(name)) {
					final Object val = data.get(name);
					it.remove();
					data.put(name, val);
				}
 			}
			
			final IResource file = getResource();
			if (outputs==null) outputs=Collections.emptyList();
			final List<String> toRead = new ArrayList<String>(outputs.size()+data.size());
			toRead.addAll(outputs);
			toRead.addAll(data.keySet());

			final Map<String,? extends Object> result = service.runScript(file.getLocation().toOSString(), data, toRead);
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
    		

		} catch (Throwable e) {
			throw createDataMessageException("Error when executing actor " + getName() + ": "
					+ e.getMessage() + "\nPlease see log for more information.", e);
		} finally {
			if (service!=null) service.stop();
		}


	}

	private DataMessageComponent getPythonJepMessage(final List<DataMessageComponent> cache) throws Exception {

		// TODO more fixing to this once the memory leak is figured out
		// FIXED We have currently circumvented Jep by using the Diamond fast RPC link.
		Jep jep=null;
		final DataMessageComponent ret = new DataMessageComponent();
		try {
			jep = new Jep();
			
			// We save the names of the abstract data sets passed in
			// these are read back and passed on sometimes.
			List<String> inputs = new ArrayList<String>(7);

			for (DataMessageComponent dataMessageComponent : cache) {
				if (dataMessageComponent.getList()!=null) {
					for (String name : dataMessageComponent.getList().keySet()) {
						final Object ob = dataMessageComponent.getList().get(name);
						if (ob instanceof AbstractDataset) {
							final AbstractDataset set = (AbstractDataset)dataMessageComponent.getList().get(name);
							NumpyUtils.setNumpy(jep, set);
							if (isPassInputs) {
								inputs.add(name);
							}
						} else {
						    jep.set(name, ob);
						}
					}
				}
				
				if (dataMessageComponent.getScalar()!=null) {
					for (String name : dataMessageComponent.getScalar().keySet()) {
						
						if (name.indexOf('.')>-1) continue;
						final String value = dataMessageComponent.getScalar().get(name);
						try {
							final int ival = Integer.parseInt(value);
							jep.set(name, ival);
							
						} catch (Throwable t) {
							try {
								final double dval = Double.parseDouble(value);
								jep.set(name, dval);
								
							} catch (Throwable t2) {
								jep.set(name, value);
							}
						}
					}
				}
			}

			final IResource file = getResource();
			if (file.exists()) jep.runScript(file.getLocation().toOSString());

			ret.addScalar(MessageUtils.getScalar(cache));
			ret.setMeta(MessageUtils.getMeta(cache));
			ret.putScalar("Python Script", getResource().getName());

			if (outputs==null) outputs=Collections.emptyList();
			final List<String> toRead = new ArrayList<String>(outputs.size()+inputs.size());
			toRead.addAll(outputs);
			toRead.addAll(inputs);
			for (String name : toRead) {
				try {
					ret.addList(name, NumpyUtils.getNumpy(jep, name));
				} catch (Throwable ne) {
					ret.putScalar(name, jep.getValue(name).toString());
				}
			}

		} catch (JepException je) {
			throw createDataMessageException("Script error in actor " + getName() + ": "
							+ je.getMessage(), je);
		} catch (Throwable e) {
			logger.error(e.getStackTrace().toString());
			throw createDataMessageException("Error when executing actor " + getName() + ": "
					+ e.getMessage() + "\nPlease see log for more information.", e);
		} finally {
			if (jep!=null) jep.close();
		}

		return ret;
		

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
				    interpreter.exec(name+" = jycore.Sciwrap("+name+")");
					if (isPassInputs) {
						inputs.add(name);
					}
				}
			}
		}
		
		final IResource file = getResource();
		if (file.exists()) interpreter.execfile(new FileInputStream(file.getLocation().toOSString()));
				
		final DataMessageComponent ret = new DataMessageComponent();		
		final Map<String,Object>  data = new HashMap<String, Object>(7);
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
			
			buf.append("from numpy import *\r\n");
			
		} else {
			
			buf.append("import "+AbstractDataset.class.getName()+" as _ds\r\n");
			buf.append("import scisoftpy as dnp\r\n");
			buf.append("import scisoftpy.core as scp\r\n");
			buf.append("import scisoftpy.maths as maths\r\n");
			buf.append("\r\n\r\n");
		
			final List<IVariable> vars = getInputVariables();
			if (vars!=null&&vars.size()>0) {
				buf.append("# Jython script for manipulating data sets.\r\n");
				buf.append("# The variables going into this actor in the workflow are available in the script.\r\n");
				buf.append("# Expected variable sets available and set when this script is run:\r\n");
				for (IVariable iVariable : vars) {
					if (iVariable.getVariableType()==VARIABLE_TYPE.ARRAY) {
						buf.append("# ");
						buf.append(iVariable.getVariableName());
						buf.append("\r\n");
					}
				}
				buf.append("\r\n# Please provide maths of these variables and other available ones in the following lines.\r\n");
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
