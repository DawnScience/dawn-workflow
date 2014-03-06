/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.digester.substitution.MultiVariableExpander;
import org.apache.commons.digester.substitution.VariableSubstitutor;
import org.dawb.common.util.io.FileUtils;

public class SubstituteUtils {

	/**
	 * Reads a UTF-8 text file with or without a BOM.
	 * 
	 * @param contents
	 * @param variables
	 * @return
	 * @throws Exception
	 */
	public static String substitute(final InputStream contents, final Map<String, String> variables) throws Exception {
		
		if (contents==null) return null;
		try {
			BufferedReader ir = new BufferedReader(new InputStreamReader(contents, "UTF-8"));
			int c;
			StringBuilder expand = new StringBuilder();
			final char[] buf = new char[4096];
			while ((c = ir.read(buf, 0, 4096)) > 0) {
				
				if (expand.length()>0) {
					if (expand.charAt(0) == FileUtils.BOM) {
						expand.delete(0, 0);
					}
				}

				expand.append(buf, 0, c);
			}
			return SubstituteUtils.substitute(expand.toString(), variables);

		} finally {
			contents.close();
		}
	}

	public static final String substitute(final String expand, final Map<String,String> variables) {
		
		if (variables==null) return expand;
		
		// Create an expander with the Map that matches ${var}
		MultiVariableExpander expander = new MultiVariableExpander( );
		expander.addSource("$", variables);
		// Create a substitutor with the expander
		VariableSubstitutor substitutor = new VariableSubstitutor(expander);
		
		return substitutor.substitute(expand);       		

	}

}
