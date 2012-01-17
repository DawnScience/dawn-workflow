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

import java.util.List;
import java.util.Map;

import org.dawb.common.util.SubstituteUtils;
import org.dawb.common.util.io.Grep;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.data.expr.Parameter;

public class ParameterUtils {

	public static final String VARIABLE_EXPRESSION = "\\$\\{([a-zA-Z0-9_ ]+)\\}";
	
	public static String getSubstituedValue(final Parameter parameter, final DataMessageComponent comp) throws Exception {
		
	    final String       stringValue = parameter.getExpression();
	    if (stringValue==null || "".equals(stringValue.trim())) return null;

	    final List<String> vars = Grep.group(stringValue, VARIABLE_EXPRESSION, 1);

		final Map<String,String> values = MessageUtils.getValues(comp, vars, parameter.getContainer());
		return SubstituteUtils.substitute(stringValue, values);
	}

	
	public static String getSubstituedValue(final Parameter parameter, final List<DataMessageComponent> cache) throws Exception {
		
	    final String       stringValue = parameter.getExpression();
	    if (stringValue==null || "".equals(stringValue.trim())) return null;
	    
		final List<String> vars = Grep.group(stringValue, VARIABLE_EXPRESSION, 1);

		final Map<String,String> values = MessageUtils.getValues(cache, vars, parameter.getContainer());
		return SubstituteUtils.substitute(stringValue, values);
	}

}
