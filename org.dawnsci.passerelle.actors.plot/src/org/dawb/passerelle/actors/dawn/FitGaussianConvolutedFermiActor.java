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
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IndexIterator;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.FermiGauss;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IParameter;

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


	private void plotFunction(AFunction fitFunction, IDataset xAxis, IDataset values) {
		String plotName = updatePlotName.getExpression();
		if (!plotName.isEmpty()) {
			try {
				AbstractDataset fermiDS = fitFunction.calculateValues(xAxis);
				SDAPlotter.plot(plotName, xAxis, new IDataset[] { fermiDS,
						values });
			} catch (Exception e) {
				// Not an important issue, as its just for display, and doesn't
				// affect the result.
			}
		}
	}
	
	private AFunction FitGaussianConvFermi(final AbstractDataset xAxis,
			final AbstractDataset values, final AFunction fitFunction) throws Exception {

		if (!(fitFunction instanceof FermiGauss)) {
			throw new IllegalArgumentException(
					"Input function must be of type FermiGauss");
		}

		String fitConvolutionValue = "Off";
		try {
			fitConvolutionValue = ParameterUtils.getSubstituedValue(fitConvolution, dataMsgComp);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		final double temperature = fitFunction.getParameterValue(1);

		//FermiGauss initialFit = fitFermiNoFWHM(xAxis, values, new FermiGauss(fitFunction.getParameters()));
		
		FermiGauss fittedFunction = new FermiGauss(fitFunction.getParameters()); // "mu", "temperature", "BG_slope", "FE_step_height", "Constant", "FWHM"
		fittedFunction.getParameter(5).setValue(0.0);
		fittedFunction.getParameter(5).setFixed(true);
		fittedFunction.getParameter(1).setFixed(false);
		
		// fit with a fixed fwhm letting the temperature vary
		try {			
			Fitter.ApacheNelderMeadFit(new AbstractDataset[] {xAxis}, values, fittedFunction);
		} catch (Exception e) {
			plotFunction(fittedFunction, xAxis, values);
			System.out.println(e);
		}

		
		int count = 0;
		while (functionsSimilarIgnoreFWHM(fittedFunction,(FermiGauss)fitFunction, 0.0) && count < 5) {
			logger.debug("Function not fitted, trying again :" + count);
			count++;
			
			try {
				
				Fitter.ApacheNelderMeadFit(new AbstractDataset[] {xAxis}, values, fittedFunction);
			} catch (Exception e) {
				plotFunction(fittedFunction, xAxis, values);
				System.out.println(e);
			}
			
		}
		
		if (count >= 5) {
			logger.debug("Fitting Failed");
		}
		
		plotFunction(fittedFunction, xAxis, values);
		
		
		fittedFunction.approximateFWHM(temperature);
		
		// return if that is all we need to do
		if (fitConvolutionValue.contains("Off")) {
			plotFunction(fittedFunction, xAxis, values);
			return fittedFunction;
		}

		plotFunction(fittedFunction, xAxis, values);
		
		// Now fit the system quickly using several assumptions
		fittedFunction.getParameters()[0].setFixed(false);
		fittedFunction.getParameters()[1].setFixed(true);
		fittedFunction.getParameters()[2].setFixed(true);
		fittedFunction.getParameters()[3].setFixed(true);
		fittedFunction.getParameters()[4].setFixed(true);
		fittedFunction.getParameters()[5].setFixed(false);
		
		try {
			
			Fitter.ApacheNelderMeadFit(new AbstractDataset[] {xAxis}, values, fittedFunction);
			
		} catch (Exception e) {
			plotFunction(fittedFunction, xAxis, values);
			System.out.println(e);
		}
		
		// if this is all that is required return the new fitted value
		if(fitConvolutionValue.contains("Quick")) {
			plotFunction(fittedFunction, xAxis, values);
			return fittedFunction;
		}

		// Now fit the system properly with the Full function
		fittedFunction.getParameters()[0].setFixed(false);
		fittedFunction.getParameters()[1].setFixed(false);
		fittedFunction.getParameters()[2].setFixed(false);
		fittedFunction.getParameters()[3].setFixed(false);
		fittedFunction.getParameters()[4].setFixed(false);
		fittedFunction.getParameters()[5].setFixed(false);
		try {
			Fitter.ApacheNelderMeadFit(new AbstractDataset[] {xAxis}, values, fittedFunction);			
		} catch (Exception e) {
			plotFunction(fittedFunction, xAxis, values);
			System.out.println(e);
		}

		plotFunction(fittedFunction, xAxis, values);
		return fittedFunction;
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
		int[] shape = dataDS.getShape();
		AFunction fitFunction = functions.get(function);
		AbstractDataset xAxisDS = null;
		if (data.containsKey(xAxis)) {
			xAxisDS = ((AbstractDataset) data.get(xAxis)).clone();
		} else {
			xAxisDS = DoubleDataset.arange(shape[fitDim], 0, -1);
		}
		
		AbstractDataset anglesAxisDS = null;
		if (data.containsKey(anglesAxis)) {
			anglesAxisDS = ((AbstractDataset) data.get(anglesAxis)).clone();
		} else {
			anglesAxisDS = DoubleDataset.arange(shape[Math.abs(fitDim-1)], 0, -1);
		}

		ArrayList<Slice> slices = new ArrayList<Slice>();
		for (int i = 0; i < shape.length; i++) {
			if (i == fitDim) {
				slices.add(new Slice(0, shape[i], 1));
			} else {
				slices.add(new Slice(0, 1, 1));
			}
		}

		ArrayList<AbstractDataset> parametersDS = new ArrayList<AbstractDataset>(
				fitFunction.getNoOfParameters());
		
		int[] lshape = shape.clone();
		lshape[fitDim] = 1;
		
		for (int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			DoubleDataset parameterDS = new DoubleDataset(lshape);
			parameterDS.squeeze();
			parametersDS.add(parameterDS);
		}

		AbstractDataset functionsDS = new DoubleDataset(shape);
		AbstractDataset residualDS = new DoubleDataset(lshape);
		residualDS.squeeze();

		int[] starts = shape.clone();
		starts[fitDim] = 1;
		DoubleDataset ind = DoubleDataset.ones(starts);
		IndexIterator iter = ind.getIterator(true);

		int maxthreads = Runtime.getRuntime().availableProcessors();
		
		ExecutorService executorService = new ThreadPoolExecutor(maxthreads,
				maxthreads, 1, TimeUnit.MINUTES,
				new ArrayBlockingQueue<Runnable>(10000, true),
				new ThreadPoolExecutor.CallerRunsPolicy());

		int[] pos = iter.getPos();
		while (iter.hasNext()) {
			logger.debug(Arrays.toString(pos));
			int[] start = pos.clone();
			int[] stop = start.clone();
			for (int i = 0; i < stop.length; i++) {
				stop[i] = stop[i] + 1;
			}
			stop[fitDim] = shape[fitDim];
			AbstractDataset slice = dataDS.getSlice(start, stop, null);
			slice.squeeze();

			FermiGauss localFitFunction = new FermiGauss(functions
					.get(function).getParameters());
			int dSlength = shape.length;
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
			double value = residualDS.getDouble(pos[0]);
			double disp = Math.abs(value-resMean);
			if (disp > resStd*3 || value <= 0) {
				logger.debug(Arrays.toString(pos));
				int[] start = pos.clone();
				int[] stop = start.clone();
				for (int i = 0; i < stop.length; i++) {
					stop[i] = stop[i] + 1;
				}
				stop[fitDim] = shape[fitDim];
				AbstractDataset slice = dataDS.getSlice(start, stop, null);
				slice.squeeze();

				FermiGauss localFitFunction = new FermiGauss(functions
						.get(function).getParameters());
				int dSlength = shape.length;
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
			try {
				fitResult = FitGaussianConvFermi(xAxisDS, slice, fitFunction);
			} catch (Exception e) {
				System.out.println(e);
			}
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
				logger.debug("Something happend during the Mu update process",e);
			}
			
			try {
				SDAPlotter.plot("Resolution", anglesAxisDS, parametersDS.get(5));
			} catch (Exception e) {
				logger.debug("Something happend during the resolution update process",e);
			}
			
			DoubleDataset resultFunctionDS = fitResult.calculateValues(xAxisDS);
			functionsDS.setSlice(resultFunctionDS, start, stop, null);
			
			AbstractDataset residual = Maths.subtract(slice, resultFunctionDS);
			residual.ipower(2);
			
			residualsDS.set(residual.sum(), position);
		}

	}

}
