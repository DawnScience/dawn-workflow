/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.override;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortMode;

public class Output extends Port {

	
	public Output(ComponentEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name, PortMode.PULL, false, true);
		setMultiport(true);
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 4100971421302507557L;

	
}
