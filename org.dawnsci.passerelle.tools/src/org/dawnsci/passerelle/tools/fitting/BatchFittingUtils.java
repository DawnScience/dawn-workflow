package org.dawnsci.passerelle.tools.fitting;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.fitting.FittingConstants;
import uk.ac.diamond.scisoft.analysis.fitting.functions.APeak;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.Gaussian;
import uk.ac.diamond.scisoft.analysis.fitting.functions.Lorentzian;
import uk.ac.diamond.scisoft.analysis.fitting.functions.PearsonVII;
import uk.ac.diamond.scisoft.analysis.fitting.functions.PseudoVoigt;
import uk.ac.diamond.scisoft.analysis.optimize.GeneticAlg;
import uk.ac.diamond.scisoft.analysis.optimize.IOptimizer;

/**
 * Warning this is a partial copy of FittingUtils. 
 * 
 * TODO move this class to scisoft analysis and make FittingUtils using this class.
 * 
 * @author fcp94556
 *
 */
public class BatchFittingUtils {

	private static Logger logger = LoggerFactory.getLogger(BatchFittingUtils.class);
	
	
	private static IPreferenceStore plottingPreferences;
	private static IPreferenceStore getPlottingPreferenceStore() {
		if (plottingPreferences!=null) return plottingPreferences;
		plottingPreferences = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.dawnsci.plotting");
		return plottingPreferences;
	}

	public static final int getPeaksRequired() {
		return getPlottingPreferenceStore().getInt(FittingConstants.PEAK_NUMBER);
	}

	/**
	 * 
	 * @param x
	 * @param peak
	 * @return
	 */
	public static final AbstractDataset[] getPeakFunction(AbstractDataset x, final AbstractDataset y, CompositeFunction peak) {

//		double min = peak.getPosition() - (peak.getFWHM()); // Quite wide
//		double max = peak.getPosition() + (peak.getFWHM());
//
//		final AbstractDataset[] a = xintersection(x,y,min,max);
//		x=a[0];
//		y=a[1];
		
//		CompositeFunction function = new CompositeFunction();
//		Offset os = new Offset(info.getY().min().doubleValue(), info.getY().max().doubleValue());
//		function.addFunction(peak);
//		function.addFunction(os);
		
		// We make an x dataset with n times the points of the real data to get a smooth
		// fitting function
		final int    factor = getPlottingPreferenceStore().getInt(FittingConstants.FIT_SMOOTH_FACTOR);
		final double xmin = x.min().doubleValue();
		final double xmax = x.max().doubleValue();
		final double step = (xmax-xmin)/(x.getSize()*factor);
		x = AbstractDataset.arange(xmin, xmax, step, AbstractDataset.FLOAT64);

		return new AbstractDataset[]{x,peak.makeDataset(x)};
		
	}

    /**
     * TODO
     * @return
     */
	public static final int getSmoothing() {
		return getPlottingPreferenceStore().getInt(FittingConstants.SMOOTHING);
	}

	public static final IOptimizer getOptimizer() {
		return new GeneticAlg(getQuality());
	}

	/**
	 * TODO
	 * @return
	 */
	private static final double getQuality() {
		return getPlottingPreferenceStore().getDouble(FittingConstants.QUALITY);
	}

	public static final APeak getPeakType() {
		try {
			
			final String peakClass = getPlottingPreferenceStore().getString(FittingConstants.PEAK_TYPE);
			
			/**
			 * Could use reflection to save on objects, but there's only 4 of them.
			 */
			return getPeakOptions().get(peakClass);
			
		} catch (Exception ne) {
			logger.error("Cannot determine peak type required!", ne);
			getPlottingPreferenceStore().setValue(FittingConstants.PEAK_TYPE, Gaussian.class.getName());
		    return new Gaussian(1, 1, 1, 1);
		}
	}	
	
	public static final Map<String, APeak> getPeakOptions() {
		final Map<String, APeak> opts = new LinkedHashMap<String, APeak>(4);
		opts.put(Gaussian.class.getName(),    new Gaussian(1, 1, 1, 1));
		opts.put(Lorentzian.class.getName(),  new Lorentzian(1, 1, 1, 1));
		opts.put(PearsonVII.class.getName(),  new PearsonVII(1, 1, 1, 1));
		opts.put(PseudoVoigt.class.getName(), new PseudoVoigt(1, 1, 1, 1));
		return opts;
	}
	
	public static final int getPolynomialOrderRequired() {
		return getPlottingPreferenceStore().getInt(FittingConstants.POLY_ORDER);
	}
	
}
