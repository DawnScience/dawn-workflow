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
