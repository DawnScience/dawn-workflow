package org.dawnsci.passerelle.tools.fitting;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static final Dataset[] getPeakFunction(Dataset x, final Dataset y, CompositeFunction peak) {

//		double min = peak.getPosition() - (peak.getFWHM()); // Quite wide
//		double max = peak.getPosition() + (peak.getFWHM());
//
//		final Dataset[] a = xintersection(x,y,min,max);
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
		x = DatasetFactory.createRange(xmin, xmax, step, Dataset.FLOAT64);

		return new Dataset[]{x,peak.calculateValues(x)};
		
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

	public static Class<? extends APeak> getPeakClass() {
		try {
			
			final String peakClass = getPlottingPreferenceStore().getString(FittingConstants.PEAK_TYPE);
			
			/**
			 * Could use reflection to save on objects, but there's only 4 of them.
			 */
			return getPeakOptions().get(peakClass);
			
		} catch (Exception ne) {
			logger.error("Cannot determine peak type required!", ne);
			getPlottingPreferenceStore().setValue(FittingConstants.PEAK_TYPE, Gaussian.class.getName());
		    return Gaussian.class;
		}
	}	
	
	public static Map<String, Class <? extends APeak>> getPeakOptions() {
		final Map<String, Class <? extends APeak>> opts = new LinkedHashMap<String, Class <? extends APeak>>(4);
		opts.put(Gaussian.class.getName(),    Gaussian.class);
		opts.put(Lorentzian.class.getName(),  Lorentzian.class);
		opts.put(PearsonVII.class.getName(),  PearsonVII.class);
		opts.put(PseudoVoigt.class.getName(), PseudoVoigt.class);
		return opts;
	}

	public static final int getPolynomialOrderRequired() {
		return getPlottingPreferenceStore().getInt(FittingConstants.POLY_ORDER);
	}
	
}
