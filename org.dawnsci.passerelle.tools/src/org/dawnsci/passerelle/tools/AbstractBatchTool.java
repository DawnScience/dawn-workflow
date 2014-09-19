/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.passerelle.tools;

import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.util.BatchToolUtils;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;


public abstract class AbstractBatchTool implements IBatchTool {

	private String batchToolId;
	
	protected List<IDataset> getPlottableData(UserPlotBean bean){
		return BatchToolUtils.getPlottableData(bean);
	}
	protected List<IDataset> getAxes(UserPlotBean bean){
		return BatchToolUtils.getImageAxes(bean);
	}

	public String getBatchToolId() {
		return batchToolId;
	}

	public void setBatchToolId(String batchToolId) {
		this.batchToolId = batchToolId;
	}
	
}
