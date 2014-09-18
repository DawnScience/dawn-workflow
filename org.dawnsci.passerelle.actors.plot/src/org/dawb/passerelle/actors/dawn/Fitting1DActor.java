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

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.dataset.Slice;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DoubleDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.IndexIterator;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.fitting.Fitter;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.optimize.ApacheNelderMead;
import uk.ac.diamond.scisoft.analysis.optimize.GeneticAlg;

public class Fitting1DActor extends AbstractDataMessageTransformer {

	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter functionName;
	public StringParameter xAxisName;
	public StringParameter fitDirection;

	public Fitting1DActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "datasetName");
		datasetName.setDisplayName("Dataset Name");
		registerConfigurableParameter(datasetName);
		
		functionName = new StringParameter(this, "functionName");
		functionName.setDisplayName("Function Name");
		registerConfigurableParameter(functionName);
		
		xAxisName = new StringParameter(this, "xAxisName");
		xAxisName.setDisplayName("Name of X-Axis");
		registerConfigurableParameter(xAxisName);
		
		fitDirection = new StringParameter(this, "fitDirection");
		fitDirection.setDisplayName("Fit Direction");
		registerConfigurableParameter(fitDirection);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws DataMessageException {
		
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		
		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();
		
		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, (Dataset) data.get(key));
		}
		
		Map<String, AFunction> functions = null;
		try {
			functions = MessageUtils.getFunctions(cache);
		} catch (Exception e1) {
			throw createDataMessageException("Failed to get the list of functions from the incomming message", e1);
		}
		
		// get the required datasets
		String dataset = datasetName.getExpression();
		String function = functionName.getExpression();
		String xAxis = xAxisName.getExpression();
		Integer fitDim = Integer.parseInt(fitDirection.getExpression());
		
		Dataset dataDS = ((Dataset)data.get(dataset)).clone();
		AFunction fitFunction = functions.get(function);
		Dataset xAxisDS = null;
		int[] shape = dataDS.getShape();
		if (data.containsKey(xAxis)) {
			xAxisDS = ((Dataset)data.get(xAxis)).clone();
		} else {
			xAxisDS = DoubleDataset.createRange(shape[fitDim],0,-1);
		}
		
		// get the general parameters from the first fit.
		ArrayList<Slice> slices = new ArrayList<Slice>();
		for (int i = 0; i < shape.length; i++) {
			if (i == fitDim) {
				slices.add(new Slice(0,shape[i], 1));
			} else {
				slices.add(new Slice(0,1,1));
			}
		}
			
		ArrayList<Dataset> parametersDS = new ArrayList<Dataset>(fitFunction.getNoOfParameters()); 
		for(int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			int[] lshape = shape.clone();
			lshape[fitDim] = 1;
			DoubleDataset parameterDS = new DoubleDataset(lshape);
			parameterDS.squeeze();
			parametersDS.add(parameterDS);
		}
		
		Dataset functionsDS = new DoubleDataset(shape);
		
		int[] starts = shape.clone();
		starts[fitDim] = 1;
		DoubleDataset ind = DoubleDataset.ones(starts);
		IndexIterator iter = ind.getIterator(true);
		int[] pos = iter.getPos();
		boolean first = true;
		
		while(iter.hasNext()) {
			System.out.println(iter.index);
			System.out.println(Arrays.toString(pos));
			int[] start = pos.clone();
			int[] stop = start.clone();
			for(int i = 0; i < stop.length; i++) {
				stop[i] = stop[i]+1;
			}
			stop[fitDim] = shape[fitDim];
			Dataset slice = dataDS.getSlice(start, stop, null);
			slice.squeeze();
			try {
				CompositeFunction fitResult = null;
				if (first) {
					fitResult = Fitter.fit(xAxisDS, slice, new GeneticAlg(0.0001), fitFunction);
					first = false;
				} else {
					fitResult = Fitter.fit(xAxisDS, slice, new ApacheNelderMead(), fitFunction);
				}
				int[] position = new int[shape.length-1];
				int count = 0;
				for(int i = 0; i < shape.length; i++) {
					if(i != fitDim) {
						position[count] = start[i];
						count++;
					}
				}
				for(int p = 0; p < fitResult.getNoOfParameters(); p++) {
					parametersDS.get(p).set(fitResult.getParameter(p).getValue(), position);
				}
				
				DoubleDataset resultFunctionDS = fitResult.calculateValues(xAxisDS);
				functionsDS.setSlice(resultFunctionDS, start, stop, null);
				
			} catch (Exception e) {
				throw createDataMessageException("Failed to fit row "+iter.index+" of the data", e);
			}
			
		}
		
		result.addList("fit_image", functionsDS);
		for(int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			result.addList("fit_parameter_"+i, parametersDS.get(i));
		}

		return result;
	}

	@Override
	protected String getOperationName() {
		return "Fit 1D data in 2D image";
	}

}
