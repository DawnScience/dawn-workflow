/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.passerelle.tools.fitting;

import java.util.List;

import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.passerelle.tools.AbstractBatchTool;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IPeak;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.IntegerDataset;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.fitting.Generic1DFitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.FunctionSquirts;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IdentifiedPeak;
import uk.ac.diamond.scisoft.analysis.optimize.IOptimizer;

/**
 * This class does batch peak fitting the same way as the peak fitting tool would.
 * 
 * It does not reference the UI at all so objects which contain peak information
 * are requires in the scisoft.analysis layer.
 * 
 * @author Matthew Gerring
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
        
        final Dataset data = DatasetUtils.convertToDataset(dataList.get(0));
        if (data.getRank()!=1) throw new Exception("Can only use "+getBatchToolId()+" with 1D data!");
        
		final double[] p1 = roi.getPointRef();
		final double[] p2 = roi.getEndPoint();

		final List<IDataset> axes = getAxes(bean);
		Dataset x  = axes!=null && !axes.isEmpty()
				           ? DatasetUtils.convertToDataset(axes.get(0))
				           : DatasetFactory.createRange(IntegerDataset.class, data.getSize());

		Dataset[] a= Generic1DFitter.selectInRange(x,data,p1[0],p2[0]);
		x = a[0]; Dataset y=a[1];

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
			
			final Dataset[] pf = BatchFittingUtils.getPeakFunction(x, y, function);
			final Dataset yp = pf[0];
			yp.setName("Peak "+ipeak);
			ret.addList(yp.getName(), yp);
			
			ipeak++;
		}

		return ret;
	}


}
