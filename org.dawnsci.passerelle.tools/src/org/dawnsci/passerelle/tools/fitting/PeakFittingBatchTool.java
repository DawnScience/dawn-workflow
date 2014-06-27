package org.dawnsci.passerelle.tools.fitting;

import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.AbstractBatchTool;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IntegerDataset;
import uk.ac.diamond.scisoft.analysis.fitting.Generic1DFitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.FunctionSquirts;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IPeak;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IdentifiedPeak;
import uk.ac.diamond.scisoft.analysis.monitor.IMonitor;
import uk.ac.diamond.scisoft.analysis.optimize.IOptimizer;
import uk.ac.diamond.scisoft.analysis.roi.LinearROI;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

/**
 * This class does batch peak fitting the same way as the peak fitting tool would.
 * 
 * It does not reference the UI at all so objects which contain peak information
 * are requires in the scisoft.analysis layer.
 * 
 * @author fcp94556
 *
 */
public class PeakFittingBatchTool extends AbstractBatchTool {

	/**
	 * We pull out just the maths of peak fitting here, disentangling the
	 * maths from the tool where possible. The peak fitting does not have a 
	 * clear path from the UI to the maths, you have to set up various preferences.
	 * Therefore there is a little work to do here:
	 */
	@Override
	public UserPlotBean process(final UserPlotBean bean, final NamedObj parent) throws Exception {
		
        final FunctionSquirts      setup = (FunctionSquirts)bean.getToolData();
        List<IdentifiedPeak>       peaks = setup.getIdentifiedPeaks();
        final RectangularROI       roi   = (RectangularROI)setup.getFitBounds();
        
        final List<IDataset> dataList = getPlottableData(bean);
        if (dataList==null || dataList.size()<1) throw new Exception("No data found for tool "+getBatchToolId());
        
        final AbstractDataset data = (AbstractDataset)dataList.get(0);
        if (data.getRank()!=1) throw new Exception("Can only use "+getBatchToolId()+" with 1D data!");
        
		final double[] p1 = roi.getPointRef();
		final double[] p2 = roi.getEndPoint();

		final List<IDataset> axes = getAxes(bean);
		AbstractDataset x  = axes!=null && !axes.isEmpty()
				           ? (AbstractDataset)axes.get(0)
				           : IntegerDataset.createRange(data.getSize());

		AbstractDataset[] a= Generic1DFitter.xintersection(x,data,p1[0],p2[0]);
		x = a[0]; AbstractDataset y=a[1];

		if (peaks==null)  {
			peaks = Generic1DFitter.parseDataDerivative(x, y, BatchFittingUtils.getSmoothing());
			setup.setIdentifiedPeaks(peaks); // We save them for later
		}

		final IOptimizer optimizer = BatchFittingUtils.getOptimizer();
		List<CompositeFunction> composites =  Generic1DFitter.fitPeakFunctions(peaks, x, y, BatchFittingUtils.getPeakClass(), optimizer, BatchFittingUtils.getSmoothing(), setup.getSquirts().size(), 0.0, false, false, new IMonitor.Stub() {
			@Override
			public boolean isCancelled() {
				return ((TypedAtomicActor)parent).getDirector().isStopRequested();
			}
		});
		
		// We set the same trace data and regions as would
		// be there if the fitting had run in the ui.
		final UserPlotBean ret = bean.clone();
		int ipeak=1; // 1 based!
		for (CompositeFunction function : composites) {
			
			final IPeak peak = function.getPeak(0);
			double w = peak.getFWHM();
			final double position = peak.getPosition();
			RectangularROI rb = new RectangularROI(position - w/2, 0, w, 0, 0);
            ret.addRoi("Peak Area "+ipeak, rb);
			ret.addRoi("Peak Line "+ipeak, new LinearROI(rb.getMidPoint(), rb.getMidPoint()));
			
			final AbstractDataset[] pf = BatchFittingUtils.getPeakFunction(x, y, function);
			final AbstractDataset yp = pf[0];
			yp.setName("Peak "+ipeak);
			ret.addList(yp.getName(), yp);
			
			ipeak++;
		}

		return ret;
	}


}
