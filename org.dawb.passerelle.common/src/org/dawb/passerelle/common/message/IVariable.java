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

public interface IVariable {

	public enum VARIABLE_TYPE {
        PATH, SCALAR, ARRAY, IMAGE, XML, ROI, FUNCTION
	}

	/**
	 * name of variable
	 * @return
	 */
	public String getVariableName();
	
	/**
	 * name of variable
	 * @return
	 */
	public Object getExampleValue();

	/**
	 * 
	 * @return
	 */
	public VARIABLE_TYPE getVariableType();
	
	/**
	 * the class of the variable for instance String, File
	 * or AbstractDataset.
	 * 
	 * @return
	 */
	public Class<?> getVariableClass();

	
	/**
	 * Returns not null if the variable has been configured incorrectly.
	 * @return
	 */
	public String getErrorMessage();
	
}
