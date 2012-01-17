/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Just a collection of suites
 * @author gerring
 *
 */
@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({ 
	
    MomlExamplesTest.class,
    MomlTest.class,
    MomlTestNoJMXService.class
})
public class Suite {
	// Run this as a junit plugin test and all the links will be satisfied.
}
