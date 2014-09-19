/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.passerelle.tools;

import org.dawb.workbench.jmx.UserPlotBean;

import ptolemy.kernel.util.NamedObj;

/**
 * 
 * A tool, which is like a plotting tool, and can run in batch from workflows.
 * 
 * @author Matthew Gerring
 *
 */
public interface IBatchTool {

	/**
	 * Call this method to process the tool the same as it would have been
	 * when run in batch. The bean  contains a method getToolData() which 
	 * the tool may cast back to data is requires to do the processing.
	 * 
	 * @param bean
	 * @return
	 */
	UserPlotBean process(UserPlotBean bean, NamedObj parent) throws Exception;

}
