/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.message;

import java.util.ArrayList;
import java.util.List;

public class VariableUtils {

	/**
	 * returns a name which has a name not contained in vars
	 * @param name
	 * @param vars
	 * @return
	 */
	public final static String getUniqueVariableName(final String name, final List<IVariable> vars) {
		
		if (vars==null || vars.isEmpty()) return name;
		
		final List<String> names = new ArrayList<String>(vars.size());
		for (IVariable v : vars) names.add(v.getVariableName());
		
		return getUniqueVariableName(name, names, 1);
	}

	private static String getUniqueVariableName(String name, List<String> names, int i) {
		if (!names.contains(name))   return name;
		if (!names.contains(name+i)) return name+i;
		return getUniqueVariableName(name, names, ++i);
	}
}
