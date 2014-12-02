/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 

package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IDataBasedFunction;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IFunction;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DoubleDataset;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;

public class FunctionToDatasetActor extends AbstractDataMessageTransformer {

	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter xAxisName;
	public StringParameter functionName;
	public StringParameter seedDataName;
	public StringParameter seedAxisName;

	public FunctionToDatasetActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "Name of the Created Dataset");
		registerConfigurableParameter(datasetName);
		xAxisName = new StringParameter(this, "Name of the X Axis dataset to create the data against");
		registerConfigurableParameter(xAxisName);
		functionName = new StringParameter(this, "Name of the function to use to create the dataset");
		registerConfigurableParameter(functionName);
		seedDataName = new StringParameter(this, "Name of the Seed dataset for the function");
		registerConfigurableParameter(seedDataName);
		seedAxisName = new StringParameter(this, "Name of the seed datasets Axis dataset");
		registerConfigurableParameter(seedAxisName);
	}
	

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws DataMessageException {
		
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		Map<String, AFunction> functions;
		try {
			functions = MessageUtils.getFunctions(cache);
		} catch (Exception e) {
			throw new DataMessageException("Could not parse the Funcitons comming into the FunctionToDatsetActor",null,e);
		}
		
		// prepare the output message
		DataMessageComponent result = MessageUtils.copy(cache);
		
		// get the required datasets
		String dataset = datasetName.getExpression();
		String xAxis = xAxisName.getExpression();
		String functionString = functionName.getExpression();

		// Get the actual objects
		Dataset xAxisDS = ((Dataset)data.get(xAxis)).clone();
		AFunction function = functions.get(functionString);
		
		populateDataBasedFunctions(data, function);
		
		// process the data
		// TODO Add Null Protection here.
		DoubleDataset createdDS = function.calculateValues(xAxisDS);
		createdDS.setName(dataset);
		
		// Add it to the result
		result.addList(createdDS.getName(), createdDS);

		return result;
	}


	private void populateDataBasedFunctions(
			final Map<String, Serializable> data, IFunction function) {
		
		if (function instanceof CompositeFunction) {
			CompositeFunction compositeFunction = (CompositeFunction) function;
			for (IFunction func : compositeFunction.getFunctions()) {
				populateDataBasedFunctions(data, func);
			}
		}
		
		if (function instanceof IDataBasedFunction) {
			IDataBasedFunction dbFunction = (IDataBasedFunction) function;

			String sdName = seedDataName.getExpression();
			String sdAxis = seedAxisName.getExpression();
			Dataset seedDataset = ((Dataset)data.get(sdName)).clone();
			Dataset seedAxisDataset = ((Dataset)data.get(sdAxis)).clone();
			dbFunction.setData(seedAxisDataset, seedDataset);
		}
	}

	@Override
	protected String getOperationName() {
		return "Make a 1D dataset from a function and an Axis.";
	}

}
