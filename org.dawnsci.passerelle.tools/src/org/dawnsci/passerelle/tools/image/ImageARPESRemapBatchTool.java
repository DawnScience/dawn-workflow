package org.dawnsci.passerelle.tools.image;

import java.util.ArrayList;
import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.AbstractBatchTool;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.InterpolatorUtils;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.dataset.function.MapToRotatedCartesian;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

/**
 * This class does image normalisation in the same way that the ImageNormalisationTool does.
 * 
 * @author ssg37927
 *
 */
public class ImageARPESRemapBatchTool extends AbstractBatchTool {

	@Override
	public UserPlotBean process(final UserPlotBean bean, final NamedObj parent) throws Exception {

		final UserPlotBean upb = (UserPlotBean)bean.getToolData();
		final ROIBase roi = (ROIBase) upb.getRois().get("mapping_roi");
		final AbstractDataset kParallel = (AbstractDataset) upb.getData().get("kParallel");
		final AbstractDataset kParaAxis = (AbstractDataset) upb.getData().get("kParaAxis");
		AbstractDataset auxiliaryData = null;
		if (upb.getData().containsKey("auxiliaryData")) {
			auxiliaryData = (AbstractDataset) upb.getData().get("auxiliaryData");
		}
		final AbstractDataset remapped_energy = (AbstractDataset) upb.getData().get("remapped_energy");
		
		final List<IDataset> dataList = getPlottableData(bean);
		if (dataList==null || dataList.size()<1) throw new Exception("No data found for tool "+getBatchToolId());

		final AbstractDataset data = (AbstractDataset)dataList.get(0);
		if (data.getRank()!=2) throw new Exception("Can only use "+getBatchToolId()+" with 2D data!");

		List<IDataset> originalAxes = getAxes(bean);
		//TODO put in some checking here.

		AbstractDataset correctedData = data;
		List<IDataset> correctedAxes = originalAxes;
		
		// Do the processing here if required
		if (auxiliaryData != null) {
			AbstractDataset newEnergyAxis = Maths.subtract(originalAxes.get(0), auxiliaryData.mean());
			AbstractDataset differences = Maths.subtract(auxiliaryData, auxiliaryData.mean());
			
			double meanSteps = (originalAxes.get(0).max().doubleValue()-originalAxes.get(0).min().doubleValue())/(float)originalAxes.get(0).getShape()[0];
			
			AbstractDataset differenceInts = Maths.floor(Maths.divide(differences, meanSteps));
			
			correctedData = new DoubleDataset(data.getShape());
			for(int y = 0; y < correctedData.getShape()[0]; y++) {
				int min = Math.max(differenceInts.getInt(y), 0);
				int max = Math.min(correctedData.getShape()[1]+differenceInts.getInt(y), correctedData.getShape()[1]);
				int ref = 0;
				for(int xx = min; xx < max; xx++) {
					correctedData.set(data.getObject(y,xx), y,ref);
					ref++;
				}
			}
			
			//correctedData = InterpolatorUtils.remapOneAxis((AbstractDataset) data, 1, (AbstractDataset) auxiliaryData, (AbstractDataset) originalAxes.get(0), newEnergyAxis);
			correctedAxes = new ArrayList<IDataset>();
			correctedAxes.add(newEnergyAxis.clone());
			correctedAxes.add(originalAxes.get(1).clone());
		}
		
		// Get the data ROI
		MapToRotatedCartesian map = new MapToRotatedCartesian((RectangularROI)roi);
		AbstractDataset dataRegion = map.value(correctedData).get(0);
		
		// prepare the results
		//AbstractDataset remappedRegion = InterpolatorUtils.remapAxis(dataRegion, 0, kParallel, kParaAxis);
		AbstractDataset remappedRegion = dataRegion;
		
		// We set the same trace data and regions as would
		// be there if the fitting had run in the ui.
		final UserPlotBean ret = bean.clone();
		
		ret.addList("remapped", remappedRegion.clone());
		ret.addList("remapped_energy", remapped_energy);
		ret.addList("remapped_k_parallel", kParaAxis);

		return ret;
	}


}
