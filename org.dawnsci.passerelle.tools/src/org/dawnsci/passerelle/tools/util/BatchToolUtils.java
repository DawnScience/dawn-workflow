package org.dawnsci.passerelle.tools.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.workbench.jmx.UserPlotBean;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;


/**
 * A class for getting data in the UserPlotBean in a form useable
 * for plotting and/or batch tools. 
 *
 * @author fcp94556
 *
 */
public class BatchToolUtils {

	/**
	 * the image axes
	 * @param bean
	 * @return
	 */
	public static List<IDataset> getImageAxes(UserPlotBean bean) {
		final List<String>    axes  = bean.getAxesNames();
		if (axes==null) return null;
		if (!bean.getData().containsKey(axes.get(0))) return null;
		if (!bean.getData().containsKey(axes.get(1))) return null;
		return Arrays.asList((IDataset)bean.getData().get(axes.get(0)), (IDataset)bean.getData().get(axes.get(1)));
	}
	
	/**
	 * The xaxis for a 1D plot.
	 * @param bean
	 * @return
	 */
	public static AbstractDataset getXAxis(UserPlotBean bean) {
		final List<String>    axes  = bean.getAxesNames();
		if (axes==null) return null;
		if (!bean.getData().containsKey(axes.get(0))) return null;
		return (AbstractDataset)bean.getData().get(axes.get(0));
	}

	/**
	 * the plottable data.
	 * @param bean
	 * @return
	 */
	public static final List<IDataset> getPlottableData(UserPlotBean bean) {
		
		if (bean.getData()!=null) {
			// Plot whatever was in the bean, if one 2D dataset is encountered use that
			// since it is exclusive.
			final Map<String,Serializable> data = new HashMap<String, Serializable>(bean.getData());
			if (bean.getDataNames()!=null && !bean.getDataNames().isEmpty()) {
				data.keySet().retainAll(bean.getDataNames());
			}
			final IDataset image = getFirst2DDataset(data);

			if (image!=null) { // We plot in 2D
				return Arrays.asList(new IDataset[]{image});
			} else { // We plot in 1D
				return get1DDatasets(data);
			}
		}
		
		return null;
	}

	private static AbstractDataset getFirst2DDataset(Map<String, Serializable> data) {
		for (String name : data.keySet()) {
			Serializable d = data.get(name);
			if (d instanceof AbstractDataset) {
				AbstractDataset dd = (AbstractDataset)d;
				dd.squeeze();
				final int rank = dd.getRank();
				if (rank==2) return dd;
			}
		}
		return null;
	}
	
	private static List<IDataset> get1DDatasets(Map<String, Serializable> data) {
		List<IDataset> ret = new ArrayList<IDataset>(7);
		for (String name : data.keySet()) {
			Serializable d = data.get(name);
			if (d instanceof AbstractDataset) {
				final int rank = ((AbstractDataset)d).getRank();
				if (rank==1) ret.add( (AbstractDataset)d );
			}
		}
		return ret;
	}


}
