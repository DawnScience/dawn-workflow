/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.data;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This class just exists to display the folder with a different icon.
 * @author gerring
 *
 */
public class FolderImportSource extends DataImportSource {

	public FolderImportSource(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name, true);
	}

}
