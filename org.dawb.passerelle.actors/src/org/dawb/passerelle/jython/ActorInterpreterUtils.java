/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.jython;

import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.AtomicActor;
import ptolemy.actor.Manager;
import uk.ac.diamond.scisoft.python.JythonInterpreterUtils;

public class ActorInterpreterUtils {

	private static Logger logger = LoggerFactory.getLogger(ActorInterpreterUtils.class);
	
	private static PythonInterpreter currentInterpreter;
	private static Manager           currentManager;
	
	/**
	 * Returns the global interpreter for this run.
	 * 
	 * This means that future nodes can use the same interpreter. This
	 * is faster as a new one is not started and previous values are
	 * available in memory.
	 * 
	 * @param actor
	 * @return
	 */
	public static PythonInterpreter getInterpreterForRun(AtomicActor actor) throws Exception {
		
		final Manager manager = actor.getManager();
		if (manager != currentManager) {
			currentManager     = null;
			if (currentInterpreter!=null) {
				currentInterpreter.cleanup();
			}
			currentInterpreter = null;
		}
		
		if (currentInterpreter==null) {
			logger.error("Creating new interpreter for "+manager.getFullName());
			currentInterpreter = JythonInterpreterUtils.getInterpreter();
			currentManager     = manager;
		}
		
		return currentInterpreter;
	}

}
