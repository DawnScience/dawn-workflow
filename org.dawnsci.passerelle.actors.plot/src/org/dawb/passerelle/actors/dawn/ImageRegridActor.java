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
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Image;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;

public class ImageRegridActor extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter xAxisPositionsName;
	public StringParameter yAxisPositionsName;
	public StringParameter linearXAxisName;
	public StringParameter linearYAxisName;

	public ImageRegridActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		xAxisPositionsName = new StringParameter(this, "xAxisPositionsName");
		registerConfigurableParameter(xAxisPositionsName);
		yAxisPositionsName = new StringParameter(this, "yAxisPositionsName");
		registerConfigurableParameter(yAxisPositionsName);
		linearXAxisName = new StringParameter(this, "linearXAxisName");
		registerConfigurableParameter(linearXAxisName);
		linearYAxisName = new StringParameter(this, "linearYAxisName");
		registerConfigurableParameter(linearYAxisName);

	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		
		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();
		
		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, DatasetFactory.createFromObject(data.get(key)));
		}
		
		// get the required datasets
		String dataset = datasetName.getExpression();
		String xAxisPositions = xAxisPositionsName.getExpression();
		String yAxisPositions = yAxisPositionsName.getExpression();
		String linearXAxis = linearXAxisName.getExpression();
		String linearYAxis = linearYAxisName.getExpression();

		Dataset dataDS = DatasetFactory.createFromObject(data.get(dataset)).clone();
		Dataset xAxisGrid = DatasetFactory.createFromObject(data.get(xAxisPositions)).clone();
		Dataset yAxisGrid = DatasetFactory.createFromObject(data.get(yAxisPositions)).clone();
		Dataset xAxisLinear = DatasetFactory.createFromObject(data.get(linearXAxis)).clone();
		Dataset yAxisLinear = DatasetFactory.createFromObject(data.get(linearYAxis)).clone();
		
		Dataset regrid = Image.regrid(dataDS, xAxisGrid, yAxisGrid, xAxisLinear, yAxisLinear);
		
		result.addList(dataset+"_regrid", regrid);
		
		// do the correction and put that into the pipeline., with a name that should be specified.
		return result;
	}

	@Override
	protected String getOperationName() {
		return "Normalise by region";
	}

}
