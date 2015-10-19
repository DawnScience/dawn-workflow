/*-
 * Copyright (c) 2014 Diamond Light Source Ltd,
 *                    European Synchrotron Radiation Facility.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.perspective;

import org.dawb.common.ui.perspective.AbstractPerspectiveLaunch;

public class WorkflowIntroLauncher extends AbstractPerspectiveLaunch {

	@Override
	public String getID() {
		return WorkflowPerspective.ID;
	}
}
