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

import java.util.List;

/**
 * An interface which provides the names of variables, usually for the outputs
 * of an actor.
 * 
 * This allows actors to provide names of outputs without having to run the node.
 * So when authoring complex workflows, it should be implemented so that nodes
 * can provide those values which are available for downstream nodes.
 * 
 * @author gerring
 *
 */
public interface IVariableProvider {

	/**
	 * List is likely to be unmodifiable.
	 * 
	 * NOTE should be implemented as UI thread safe, can be called from anywhere.
	 * 
	 * @return
	 */
	public List<IVariable> getOutputVariables();
	
	/**
	 * List is likely to be unmodifiable.
	 * 
	 * NOTE should be implemented as UI thread safe, can be called from anywhere.
	 * 
	 * @return
	 */
	public List<IVariable> getInputVariables();

}
