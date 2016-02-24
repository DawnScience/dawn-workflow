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
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
import org.eclipse.dawnsci.analysis.dataset.impl.DoubleDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Maths;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.fitting.functions.FermiGauss;

public class PrepareFermiFittingFunctionsActor extends
		AbstractDataMessageTransformer {
	
	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter xAxisName;
	public StringParameter temperature;

	public PrepareFermiFittingFunctionsActor(CompositeEntity container,
			String name) throws NameDuplicationException,
			IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		xAxisName = new StringParameter(this, "xAxisName");
		registerConfigurableParameter(xAxisName);
		temperature = new StringParameter(this, "Temperature");
		registerConfigurableParameter(temperature);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws DataMessageException {

		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable> data = MessageUtils.getList(cache);

		// prepare the output message
		DataMessageComponent result = MessageUtils.copy(cache);

		// get the required datasets
		String dataset = datasetName.getExpression();
		String xAxis = xAxisName.getExpression();
		
		Dataset dataDS = DatasetFactory.createFromObject(data.get(dataset)).clone();
		int[] shape = dataDS.getShape();
		
		Dataset xAxisDS = null;
		if (data.containsKey(xAxis)) {
			xAxisDS = DatasetFactory.createFromObject(data.get(xAxis)).clone();
		} else {
			xAxisDS = DoubleDataset.createRange(shape[0], 0, -1);
		}
		
		Double temperatureValue = 10.0;
		try {
			String temperatureValueString = ParameterUtils.getSubstituedValue(temperature, result);
			temperatureValue = Double.parseDouble(temperatureValueString);
		} catch (Exception e) {
			// TODO: Should log something.
		}
		
		FermiGauss fg = new FermiGauss();
		
		// Mu
		double min = dataDS.min(true).doubleValue();
		double height = dataDS.max(true).doubleValue() - min;
		int crossing = Maths.abs(Maths.subtract(dataDS, (min+(height/2.0)))).minPos()[0];

		fg.getParameter(0).setValue(xAxisDS.getDouble(crossing));
		fg.getParameter(0).setLowerLimit(xAxisDS.min(true).doubleValue());
		fg.getParameter(0).setUpperLimit(xAxisDS.max(true).doubleValue());
		
		// Temperature
		fg.getParameter(1).setValue(temperatureValue);
		fg.getParameter(1).setLowerLimit(0.0);
		fg.getParameter(1).setUpperLimit(300.0);
		fg.getParameter(1).setFixed(true);
		
		// BG Slope
		fg.getParameter(2).setValue(0.0);
		fg.getParameter(2).setLowerLimit(-10000.0);
		fg.getParameter(2).setUpperLimit(10000.0);
		
		// Step Height
		fg.getParameter(3).setValue(height);
		fg.getParameter(3).setLowerLimit(0.0);
		fg.getParameter(3).setUpperLimit(height*2);
		
		// Constant
		fg.getParameter(4).setValue(min);
		fg.getParameter(4).setLowerLimit(0.0);
		fg.getParameter(4).setUpperLimit(min*2);
		
		// FWHM
		fg.getParameter(5).setValue(0.001);
		fg.getParameter(5).setLowerLimit(0.001);
		fg.getParameter(5).setUpperLimit(0.1);
		
		
		
		result.addFunction("fermi", fg);

		// Update the names of the axis so that plotting works more nicely later on
		xAxisDS.setName("Energy");
		dataDS.setName("Intensity");
		result.addList(dataset, dataDS);
		result.addList(xAxis, xAxisDS);
		
		return result;
	}

	@Override
	protected String getOperationName() {
		return "Generate estimaged fermi functions from data";
	}

}