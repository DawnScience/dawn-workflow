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
import java.util.Map;

import org.dawb.passerelle.common.message.IVariable;

public interface SubstitutionParticipant {

	/**
	 * Use to reset editor 
	 */
	public String getDefaultSubstitution();

	/**
	 * Used to define things that can be inserted
	 * @return
	 */
	public List<IVariable> getInputVariables();

	/**
	 * Return the values used in the substitution
	 * @return
	 */
	public Map<String, String> getExampleValues();
}
