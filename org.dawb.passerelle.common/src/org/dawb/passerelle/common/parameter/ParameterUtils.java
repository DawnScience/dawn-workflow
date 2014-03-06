/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.parameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.substitution.MultiVariableExpander;
import org.apache.commons.digester.substitution.VariableSubstitutor;
import org.dawb.common.util.io.Grep;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.utils.SubstituteUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;

import com.isencia.passerelle.workbench.model.utils.ModelUtils;

import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.NamedObj;

public class ParameterUtils {

	public static final String VARIABLE_EXPRESSION = "\\$\\{([a-zA-Z0-9_ \\.]+)\\}";


	public static String getSubstituedValue(final Parameter parameter) throws Exception {
		return getSubstituedValue(parameter, (DataMessageComponent)null);
	}

	public static String getSubstituedValue(final Parameter parameter, final DataMessageComponent comp) throws Exception {
		
	    String       stringValue = parameter.getExpression();
	    if (stringValue==null || "".equals(stringValue.trim())) return null;

	    final List<String> vars = Grep.group(stringValue, VARIABLE_EXPRESSION, 1);

		final Map<String,String> values = MessageUtils.getValues(comp, vars, parameter.getContainer());
		
		// Commented out while waiting for a fix
//		try {
//			VariablesPlugin.getDefault().getStringVariableManager().validateStringVariables(stringValue);
//		    stringValue = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(stringValue, false);
//		} catch (Throwable ignored) {
//			// We just try any eclipse vars
//		}
		stringValue = SubstituteUtils.substitute(stringValue, values);
		return stringValue;
	}

	public static String getSubstituedValue(final Parameter parameter, final List<DataMessageComponent> cache) throws Exception {

		return getSubstituedValue(parameter.getExpression(), parameter.getContainer(), cache);
	}
	
	public static String getSubstituedValue(final String stringValue, final NamedObj container, final List<DataMessageComponent> cache) throws Exception {

		if (stringValue==null || "".equals(stringValue.trim())) return null;

		final List<String> vars = Grep.group(stringValue, VARIABLE_EXPRESSION, 1);

		final Map<String,String> values = MessageUtils.getValues(cache, vars, container);
		
		// Commented out while waiting for a fix
//		try {
//			VariablesPlugin.getDefault().getStringVariableManager().validateStringVariables(stringValue);
//		    stringValue = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(stringValue, false);
//		} catch (Throwable ignored) {
//			// We just try any eclipse vars
//		}
		return SubstituteUtils.substitute(stringValue, values);
	}


	/**
	 * substutes variables in the string as determined from the actor.
	 * 
	 * @param filePath
	 * @param dataExportTransformer
	 * @throws Exception 
	 */
	public static String substitute(String stringValue, final NamedObj actor) throws Exception {

		final Map<String, Object> variables = new HashMap<String, Object>(3);
		if (actor != null) {
			if (ModelUtils.getProject(actor) != null) {
				variables.put("project_name", ModelUtils.getProject(actor).getName());				
			}
			variables.put("actor_name", actor.getName());
		}

		MultiVariableExpander expander = new MultiVariableExpander();
		expander.addSource("$", variables);
		// Create a substitutor with the expander
		VariableSubstitutor substitutor = new VariableSubstitutor(expander);

		// Commented out while waiting for a fix
//		try {
//			VariablesPlugin.getDefault().getStringVariableManager().validateStringVariables(stringValue);
//		    stringValue = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(stringValue, false);
//		} catch (Throwable ignored) {
//			// We just try any eclipse vars
//		}
		return substitutor.substitute(stringValue);
	}

	public static void setEclipseValueVariable(String name, String value) throws CoreException {

		IValueVariable var = VariablesPlugin.getDefault().getStringVariableManager().getValueVariable(name);

		if (var == null) {
			IValueVariable[] variables = new IValueVariable[1];
			variables[0] = new UtilValueVariable(name, "Util setup variable:"+name, value);
			VariablesPlugin.getDefault().getStringVariableManager().addVariables(variables);
		} else {
			var.setValue(value);
		}
	}
}
