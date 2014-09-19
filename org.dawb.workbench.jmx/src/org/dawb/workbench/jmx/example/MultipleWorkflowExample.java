/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.example;


/**
 * This class is just an example to show how to run the workflow
 * remotely.
 * 
 * @author gerring
 *
 */
public class MultipleWorkflowExample extends WorkflowExample {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		runWorkflow();
		
		runWorkflow();

		System.exit(1);
	}

}
