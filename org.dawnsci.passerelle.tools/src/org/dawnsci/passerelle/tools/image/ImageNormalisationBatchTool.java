package org.dawnsci.passerelle.tools.image;

import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.AbstractBatchTool;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;

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
		final Dataset norm = (Dataset) upb.getData().get("norm_correction");
		
		final List<IDataset> dataList = getPlottableData(bean);
		if (dataList==null || dataList.size()<1) throw new Exception("No data found for tool "+getBatchToolId());

		final Dataset data = (Dataset)dataList.get(0);
		if (data.getRank()!=2) throw new Exception("Can only use "+getBatchToolId()+" with 2D data!");


		// We set the same trace data and regions as would
		// be there if the fitting had run in the ui.
		final UserPlotBean ret = bean.clone();

		ret.addList("norm", Maths.divide(data, norm));

		return ret;
	}


}
