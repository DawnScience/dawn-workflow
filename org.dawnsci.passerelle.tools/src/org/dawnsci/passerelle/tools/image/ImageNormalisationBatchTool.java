/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.passerelle.tools.image;

import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.AbstractBatchTool;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.Maths;

import ptolemy.kernel.util.NamedObj;

/**
 * This class does image normalisation in the same way that the ImageNormalisationTool does.
 * 
 * @author ssg37927
 *
 */
public class ImageNormalisationBatchTool extends AbstractBatchTool {

	@Override
	public UserPlotBean process(final UserPlotBean bean, final NamedObj parent) throws Exception {

		final UserPlotBean upb = (UserPlotBean)bean.getToolData();
		final Dataset norm = DatasetFactory.createFromObject(upb.getData().get("norm_correction"));
		
		final List<IDataset> dataList = getPlottableData(bean);
		if (dataList==null || dataList.size()<1) throw new Exception("No data found for tool "+getBatchToolId());

		final IDataset data = dataList.get(0);
		if (data.getRank()!=2) throw new Exception("Can only use "+getBatchToolId()+" with 2D data!");


		// We set the same trace data and regions as would
		// be there if the fitting had run in the ui.
		final UserPlotBean ret = bean.clone();

		ret.addList("norm", Maths.divide(data, norm));

		return ret;
	}


}
