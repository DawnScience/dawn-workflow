/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.editors;

import java.util.List;

import org.dawb.passerelle.common.message.IVariable;

public interface XPathParticipant {

	/**
	 * Tests if variable comes from upstream
	 * @param variableName
	 * @return
	 */
	boolean isUpstreamVariable(String variableName);

	/**
	 * Returns participant name, used in error messaging.
	 * @return
	 */
	String getName();

	/**
	 * 
	 * @param getxPath
	 * @param rename
	 * @return
	 */
	String getExampleValue(String getxPath, String rename);

	/**
	 * Output from partipant
	 * @return
	 */
	List<IVariable> getXPathVariables();

	
	/**
	 * Output from partipant
	 * @return
	 */
	List<IVariable> getUpstreamVariables();

}
