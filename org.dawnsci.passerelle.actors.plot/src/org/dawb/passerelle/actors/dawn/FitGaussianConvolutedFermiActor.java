/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IndexIterator;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.FermiGauss;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IParameter;

@SuppressWarnings("deprecation")
//TODO there are lots of things in apache commons maths which are depricated
public class FitGaussianConvolutedFermiActor extends
AbstractDataMessageTransformer {

	private static final Logger logger = LoggerFactory.getLogger(FitGaussianConvolutedFermiActor.class);
	
	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter functionName;
	public StringParameter xAxisName;
	public StringParameter anglesAxisName;
	public StringParameter fitDirection;
	public StringParameter fitConvolution;
	public StringParameter updatePlotName;
	// TODO can be removed
	public StringParameter quickConvolutionWidth;

	public FitGaussianConvolutedFermiActor(CompositeEntity container,
			String name) throws NameDuplicationException,
			IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		functionName = new StringParameter(this, "functionName");
		registerConfigurableParameter(functionName);
		xAxisName = new StringParameter(this, "xAxisName");
		registerConfigurableParameter(xAxisName);
		anglesAxisName = new StringParameter(this, "anglesAxisName");
		registerConfigurableParameter(anglesAxisName);
		fitDirection = new StringParameter(this, "fitDirection");
		registerConfigurableParameter(fitDirection);
		fitConvolution = new StringParameter(this, "fitConvolution");
//		fitConvolution.addChoice("Off");
//		fitConvolution.addChoice("Quick");
//		fitConvolution.addChoice("Full");
		registerConfigurableParameter(fitConvolution);
		updatePlotName = new StringParameter(this, "updatePlotName");
		registerConfigurableParameter(updatePlotName);
		quickConvolutionWidth = new StringParameter(this,
				"quickConvolutionWidth");
		registerConfigurableParameter(quickConvolutionWidth);
		
	}


	private FermiGauss fitFermiNoFWHM(final AbstractDataset xAxis, final AbstractDataset observed, final FermiGauss fitFunction) {

		MultivariateFunction f = new MultivariateFunction() {
			@Override
			public double value(double[] p) {
				double p0 = deNormalizeParameter(p[0],
						fitFunction.getParameter(0));
				double p1 = deNormalizeParameter(p[1],
						fitFunction.getParameter(1));
				double p2 = deNormalizeParameter(p[2],
						fitFunction.getParameter(2));
				double p3 = deNormalizeParameter(p[3],
						fitFunction.getParameter(3));
				double p4 = deNormalizeParameter(p[4],
						fitFunction.getParameter(4));

				FermiGauss fg = new FermiGauss(p0, p1, p2, p3, p4, 0.0);

				AbstractDataset fermiDS = fg.getFermiDS(xAxis);

				AbstractDataset residual = Maths.subtract(fermiDS, observed);
				residual.ipower(2);

				return (Double) residual.sum();
			}
		};

		// perform the optimisation
		double[] start = new double[] {
				normalizeParameter(fitFunction.getParameterValue(0),fitFunction.getParameter(0)),
				normalizeParameter(fitFunction.getParameterValue(1),fitFunction.getParameter(1)),
				normalizeParameter(fitFunction.getParameterValue(2),fitFunction.getParameter(2)),
				normalizeParameter(fitFunction.getParameterValue(3),fitFunction.getParameter(3)),
				normalizeParameter(fitFunction.getParameterValue(4),fitFunction.getParameter(4)) };

		CMAESOptimizer cOpt = new CMAESOptimizer(5);
		PointValuePair result = cOpt
				.optimize(2000, f, GoalType.MINIMIZE, start);

		// set the input functions parameters to be the result before finishing.
		double[] r = result.getPoint();

		double r0 = deNormalizeParameter(r[0], fitFunction.getParameter(0));
		double r1 = deNormalizeParameter(r[1], fitFunction.getParameter(1));
		double r2 = deNormalizeParameter(r[2], fitFunction.getParameter(2));
		double r3 = deNormalizeParameter(r[3], fitFunction.getParameter(3));
		double r4 = deNormalizeParameter(r[4], fitFunction.getParameter(4));
		double r5 = 0.0;

		FermiGauss returnFunction = new FermiGauss(fitFunction.getParameters());
		returnFunction.setParameterValues(r0,r1,r2,r3,r4,r5);
		return returnFunction;
	}


	private FermiGauss fitFermiOnlyFWHM(final AbstractDataset xAxis, final AbstractDataset observed, final FermiGauss fitFunction) {

		final double f0 = fitFunction.getParameterValue(0);
		final double f1 = fitFunction.getParameterValue(1);
		final double f2 = fitFunction.getParameterValue(2);
		final double f3 = fitFunction.getParameterValue(3);
		final double f4 = fitFunction.getParameterValue(4);

		MultivariateFunction f = new MultivariateFunction() {

			@Override
			public double value(double[] p) {

				double p0 = deNormalizeParameter(p[0],
						fitFunction.getParameter(5));

				FermiGauss fg = new FermiGauss(f0, f1, f2, f3, f4, p0);

				AbstractDataset fermiDS = fg.makeDataset(xAxis);

				AbstractDataset residual = Maths.subtract(fermiDS,
						observed);

				residual.ipower(2);

				return (Double) residual.sum();
			}
		};

		// perform the optimisation
		double[] start = new double[] { 
				normalizeParameter(fitFunction.getParameterValue(5),fitFunction.getParameter(5)) };

		CMAESOptimizer cOpt = new CMAESOptimizer(5);
		PointValuePair result = cOpt.optimize(1000, f, GoalType.MINIMIZE, start);

		// set the input functions parameters to be the result before
		// finishing.
		double[] r = result.getPoint();

		double r5 = deNormalizeParameter(Math.abs(r[0]),
				fitFunction.getParameter(5));

		FermiGauss returnFunction = new FermiGauss(fitFunction.getParameters());
		returnFunction.setParameterValues(f0,f1,f2,f3,f4,r5);
		return returnFunction;
	}

	private FermiGauss fitFermiFixedTemp(final AbstractDataset xAxis, final AbstractDataset observed, final FermiGauss fitFunction) {

		final double f1 = fitFunction.getParameterValue(1);

		MultivariateFunction f = new MultivariateFunction() {
			@Override
			public double value(double[] p) {
				double p0 = deNormalizeParameter(p[0],
						fitFunction.getParameter(0));
				// Missing the temperature (parameter 1)  as this is fixed
				double p2 = deNormalizeParameter(p[1],
						fitFunction.getParameter(2));
				double p3 = deNormalizeParameter(p[2],
						fitFunction.getParameter(3));
				double p4 = deNormalizeParameter(p[3],
						fitFunction.getParameter(4));
				double p5 = deNormalizeParameter(p[4],
						fitFunction.getParameter(5));

				FermiGauss fg = new FermiGauss(p0, f1, p2, p3, p4, p5);

				AbstractDataset fermiDS = fg.makeDataset(xAxis);

				AbstractDataset residual = Maths.subtract(fermiDS, observed);
				residual.ipower(2);

				return (Double) residual.sum();
			}
		};

		// perform the optimisation
		double[] start = new double[] {
				normalizeParameter(fitFunction.getParameterValue(0),fitFunction.getParameter(0)),
				// Missing the temperature (parameter 1)  as this is fixed
				normalizeParameter(fitFunction.getParameterValue(2),fitFunction.getParameter(2)),
				normalizeParameter(fitFunction.getParameterValue(3),fitFunction.getParameter(3)),
				normalizeParameter(fitFunction.getParameterValue(4),fitFunction.getParameter(4)),
				normalizeParameter(fitFunction.getParameterValue(5),fitFunction.getParameter(5)) };

		CMAESOptimizer cOpt = new CMAESOptimizer(5);
		PointValuePair result = cOpt
				.optimize(2000, f, GoalType.MINIMIZE, start);

		// set the input functions parameters to be the result before finishing.
		double[] r = result.getPoint();

		double r0 = deNormalizeParameter(r[0], fitFunction.getParameter(0));
		// Missing the temperature (parameter 1)  as this is fixed
		double r2 = deNormalizeParameter(r[1], fitFunction.getParameter(2));
		double r3 = deNormalizeParameter(r[2], fitFunction.getParameter(3));
		double r4 = deNormalizeParameter(r[3], fitFunction.getParameter(4));
		double r5 = deNormalizeParameter(r[4], fitFunction.getParameter(5));;

		FermiGauss returnFunction = new FermiGauss(fitFunction.getParameters());
		returnFunction.setParameterValues(r0,f1,r2,r3,r4,r5);
		return returnFunction;

	}

	private void plotFunction(AFunction fitFunction, IDataset xAxis, IDataset values) {
		String plotName = updatePlotName.getExpression();
		if (!plotName.isEmpty()) {
			try {
				AbstractDataset fermiDS = fitFunction.makeDataset(xAxis);
				SDAPlotter.plot(plotName, xAxis, new IDataset[] { fermiDS,
						values });
			} catch (Exception e) {
				// Not an important issue, as its just for display, and doesn't
				// affect the result.
			}
		}
	}
	
	private AFunction FitGaussianConvFermi(final AbstractDataset xAxis,
			final AbstractDataset values, final AFunction fitFunction) {

		if (!(fitFunction instanceof FermiGauss)) {
			throw new IllegalArgumentException(
					"Input function must be of type FermiGauss");
		}

		String fitConvolutionValue = "Off";
		try {
			fitConvolutionValue = ParameterUtils.getSubstituedValue(fitConvolution, cache);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		final double temperature = fitFunction.getParameterValue(1);

		FermiGauss initialFit = fitFermiNoFWHM(xAxis, values, new FermiGauss(fitFunction.getParameters()));

		int count = 0;
		while (functionsSimilarIgnoreFWHM(initialFit,(FermiGauss)fitFunction, 0.0) && count < 5) {
			logger.debug("Function not fitted, trying again :" + count);
			count++;
			
			initialFit = fitFermiNoFWHM(xAxis, values, new FermiGauss(fitFunction.getParameters()));
		}
		
		if (count >= 5) {
			logger.debug("Fitting Failed");
		}
		
		
		// return if that is all we need to do
		if (fitConvolutionValue.contains("Off")) {
			plotFunction(initialFit, xAxis, values);
			return initialFit;
		}

		// Now fit the system quickly using several assumptions

		// get the approximate fwhm and required width for calculation
		double width = initialFit.approximateFWHM(temperature);

		double fermiEdge = initialFit.getParameterValue(0);
		
		// pull out the appropriate slice
		Integer sliceStop = xAxis.getShape()[0];
		Integer sliceStart = 0;
		try {
			
			Double afterCrossing = DatasetUtils.crossings(xAxis, fermiEdge + width)
					.get(0);
			sliceStop = (int) Math.floor(afterCrossing);
		} catch (Exception e) {
			// Not an issue as this is handled
			logger.debug("Minnor Issue occured, but this is handled, simply put here for reporting",e);
		}

		try {
			Double beforeCrossing = DatasetUtils.crossings(xAxis, fermiEdge - width)
					.get(0);
			sliceStart = (int) Math.floor(beforeCrossing);
		} catch (Exception e) {
			// Not an issue as this is not required
			logger.debug("Minnor Issue occured, but this is handled, simply put here for reporting",e);
		}

		final AbstractDataset trimmedXAxis = xAxis.getSlice(new Slice(
				sliceStart, sliceStop));
		final AbstractDataset trimmedValues = values.getSlice(new Slice(
				sliceStart, sliceStop));


		// set up the temperature and approximate FWHM correctly
		//initialFit.getParameter(1).setValue(temperature);
		//initialFit.getParameter(5).setValue(approximateFWHM);
		FermiGauss fwhmQuickFit = fitFermiOnlyFWHM(trimmedXAxis, trimmedValues, initialFit);

		// if this is all that is required return the new fitted value
		if(fitConvolutionValue.contains("Quick")) {
			plotFunction(fwhmQuickFit, trimmedXAxis, trimmedValues);
			return fwhmQuickFit;
		}

		// Now fit the system properly with the Full function
		FermiGauss fullFit = fitFermiFixedTemp(xAxis, values, fwhmQuickFit);

		plotFunction(fullFit, xAxis, values);
		return fullFit;
	}

	private boolean functionsSimilarIgnoreFWHM(FermiGauss initialFit,
			FermiGauss fitFunction, double tollerence) {
		for (int i = 0; i < 5; i++) {
			if (Math.abs(fitFunction.getParameterValue(i)-initialFit.getParameterValue(i)) <= tollerence) return true;
			if (Math.abs(fitFunction.getParameter(i).getLowerLimit()-initialFit.getParameterValue(i)) <= tollerence) return true;
			if (Math.abs(fitFunction.getParameter(i).getUpperLimit()-initialFit.getParameterValue(i)) <= tollerence) return true;
		}
		return false;
	}


	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws DataMessageException {

		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable> data = MessageUtils.getList(cache);

		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();

		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, (AbstractDataset) data.get(key));
		}

		Map<String, AFunction> functions = null;
		try {
			functions = MessageUtils.getFunctions(cache);
		} catch (Exception e1) {
			throw createDataMessageException(
					"Failed to get the list of functions from the incomming message",
					e1);
		}

		// get the required datasets
		String dataset = datasetName.getExpression();
		String function = functionName.getExpression();
		String xAxis = xAxisName.getExpression();
		String anglesAxis = anglesAxisName.getExpression();
		Integer fitDim = Integer.parseInt(fitDirection.getExpression());

		AbstractDataset dataDS = ((AbstractDataset) data.get(dataset)).clone();
		AFunction fitFunction = functions.get(function);
		AbstractDataset xAxisDS = null;
		if (data.containsKey(xAxis)) {
			xAxisDS = ((AbstractDataset) data.get(xAxis)).clone();
		} else {
			xAxisDS = DoubleDataset.arange(dataDS.getShape()[fitDim], 0, -1);
		}
		
		AbstractDataset anglesAxisDS = null;
		if (data.containsKey(anglesAxis)) {
			anglesAxisDS = ((AbstractDataset) data.get(anglesAxis)).clone();
		} else {
			anglesAxisDS = DoubleDataset.arange(dataDS.getShape()[Math.abs(fitDim-1)], 0, -1);
		}

		ArrayList<Slice> slices = new ArrayList<Slice>();
		for (int i = 0; i < dataDS.getShape().length; i++) {
			if (i == fitDim) {
				slices.add(new Slice(0, dataDS.getShape()[i], 1));
			} else {
				slices.add(new Slice(0, 1, 1));
			}
		}

		ArrayList<AbstractDataset> parametersDS = new ArrayList<AbstractDataset>(
				fitFunction.getNoOfParameters());
		
		int[] shape = dataDS.getShape().clone();
		shape[fitDim] = 1;
		
		for (int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			DoubleDataset parameterDS = new DoubleDataset(shape);
			parameterDS.squeeze();
			parametersDS.add(parameterDS);
		}

		AbstractDataset functionsDS = new DoubleDataset(dataDS.getShape());
		AbstractDataset residualDS = new DoubleDataset(shape);
		residualDS.squeeze();

		int[] starts = dataDS.getShape().clone();
		starts[fitDim] = 1;
		DoubleDataset ind = DoubleDataset.ones(starts);
		IndexIterator iter = ind.getIterator();

		int maxthreads = Runtime.getRuntime().availableProcessors();
		
		ExecutorService executorService = new ThreadPoolExecutor(maxthreads,
				maxthreads, 1, TimeUnit.MINUTES,
				new ArrayBlockingQueue<Runnable>(10000, true),
				new ThreadPoolExecutor.CallerRunsPolicy());

		while (iter.hasNext()) {
			logger.debug(Arrays.toString(ind.getNDPosition(iter.index)));
			int[] start = ind.getNDPosition(iter.index).clone();
			int[] stop = start.clone();
			for (int i = 0; i < stop.length; i++) {
				stop[i] = stop[i] + 1;
			}
			stop[fitDim] = dataDS.getShape()[fitDim];
			AbstractDataset slice = dataDS.getSlice(start, stop, null);
			slice.squeeze();

			FermiGauss localFitFunction = new FermiGauss(functions
					.get(function).getParameters());
			int dSlength = dataDS.getShape().length;
			executorService.submit(new Worker(localFitFunction, xAxisDS, anglesAxisDS, slice,
					dSlength, start, stop, fitDim, parametersDS, functionsDS, residualDS));
		}

		// TODO possibly add more fault tolerance here
		executorService.shutdown();
		try {
			executorService.awaitTermination(10, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// Now have a look at the residuals, and see if any are particularly bad (or zero)
		double resMean = (Double) residualDS.mean();
		double resStd = (Double) residualDS.stdDeviation();
		iter.reset();

		executorService = new ThreadPoolExecutor(maxthreads,
				maxthreads, 1, TimeUnit.MINUTES,
				new ArrayBlockingQueue<Runnable>(10000, true),
				new ThreadPoolExecutor.CallerRunsPolicy());

		while (iter.hasNext()) {
			double value = residualDS.getDouble(iter.index);
			double disp = Math.abs(value-resMean);
			if (disp > resStd*3) {
				logger.debug(Arrays.toString(ind.getNDPosition(iter.index)));
				int[] start = ind.getNDPosition(iter.index).clone();
				int[] stop = start.clone();
				for (int i = 0; i < stop.length; i++) {
					stop[i] = stop[i] + 1;
				}
				stop[fitDim] = dataDS.getShape()[fitDim];
				AbstractDataset slice = dataDS.getSlice(start, stop, null);
				slice.squeeze();

				FermiGauss localFitFunction = new FermiGauss(functions
						.get(function).getParameters());
				int dSlength = dataDS.getShape().length;
				executorService.submit(new Worker(localFitFunction, xAxisDS, anglesAxisDS, slice,
						dSlength, start, stop, fitDim, parametersDS, functionsDS, residualDS));
			}
		}


		// TODO possibly add more fault tolerance here
		executorService.shutdown();
		try {
			executorService.awaitTermination(10, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		result.addList("fit_image", functionsDS);
		result.addList("fit_residuals", residualDS);
		for (int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			result.addList("fit_parameter_" + i, parametersDS.get(i));
		}

		return result;
	}

	@Override
	protected String getOperationName() {
		return "Fit 1D data in 2D image";
	}

	/**
	 * Takes in a real value and returns a value between 0 and 1; this value can
	 * be reverted using deNormalizeParameter
	 * 
	 * @param value
	 * @param iParameter
	 * @return
	 */
	public static double normalizeParameter(double value, IParameter iParameter) {
		double max = iParameter.getUpperLimit();
		double min = iParameter.getLowerLimit();
		double range = max - min;
		return (value - min) / range;
	}

	/**
	 * 
	 * @param value
	 * @param iParameter
	 * @return
	 */
	public static double deNormalizeParameter(double value,
			IParameter iParameter) {
		double max = iParameter.getUpperLimit();
		double min = iParameter.getLowerLimit();
		double range = max - min;
		return (value * range) + min;
	}

	private class Worker implements Runnable {

		private AFunction fitFunction;
		private AbstractDataset xAxisDS;
		private AbstractDataset anglesAxisDS;
		private AbstractDataset slice;
		private int DSlength;
		private int[] start;
		private int[] stop;
		private int fitDim;
		private ArrayList<AbstractDataset> parametersDS;
		private AbstractDataset functionsDS;
		private AbstractDataset residualsDS;

		public Worker(AFunction fitFunction, AbstractDataset xAxisDS, AbstractDataset anglesAxisDS,
				AbstractDataset slice, int dSlength, int[] start, int[] stop,
				int fitDim, ArrayList<AbstractDataset> parametersDS,
				AbstractDataset functionsDS, AbstractDataset residualsDS) {
			super();
			this.fitFunction = fitFunction;
			this.xAxisDS = xAxisDS;
			this.anglesAxisDS = anglesAxisDS;
			this.slice = slice;
			DSlength = dSlength;
			this.start = start;
			this.stop = stop;
			this.fitDim = fitDim;
			this.parametersDS = parametersDS;
			this.functionsDS = functionsDS;
			this.residualsDS = residualsDS;
		}

		@Override
		public void run() {
			AFunction fitResult = null;
			fitResult = FitGaussianConvFermi(xAxisDS, slice, fitFunction);
			int[] position = new int[DSlength - 1];
			int count = 0;
			for (int i = 0; i < DSlength; i++) {
				if (i != fitDim) {
					position[count] = start[i];
					count++;
				}
			}
			
			for (int p = 0; p < fitResult.getNoOfParameters(); p++) {
				parametersDS.get(p).set(fitResult.getParameter(p).getValue(),
						position);
			}

			try {
				SDAPlotter.plot("Mu", anglesAxisDS, parametersDS.get(0));
			} catch (Exception e) {
				logger.debug("Soething happend during the Mu update process",e);
			}
			
			DoubleDataset resultFunctionDS = fitResult.makeDataset(xAxisDS);
			functionsDS.setSlice(resultFunctionDS, start, stop, null);
			
			AbstractDataset residual = Maths.subtract(slice, resultFunctionDS);
			residual.ipower(2);
			
			residualsDS.set(residual.sum(), position);
		}

	}

}
