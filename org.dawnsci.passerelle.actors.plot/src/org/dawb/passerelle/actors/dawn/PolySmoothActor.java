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
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.optimize.ApachePolynomial;

public class PolySmoothActor extends AbstractDataMessageTransformer {

	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter xAxisName;
	public StringParameter windowSizeParam;
	public StringParameter polyOrderParam;

	public PolySmoothActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		xAxisName = new StringParameter(this, "xAxisName");
		registerConfigurableParameter(xAxisName);
		windowSizeParam = new StringParameter(this, "windowSizeParam");
		registerConfigurableParameter(windowSizeParam);
		polyOrderParam = new StringParameter(this, "polyOrderParam");
		registerConfigurableParameter(polyOrderParam);
		
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
			result.addList(key, (AbstractDataset) data.get(key));
		}
		
		// get the required datasets
		String dataset = datasetName.getExpression();
		String xAxis = xAxisName.getExpression();
		int windowSize = Integer.parseInt(windowSizeParam.getExpression());
		int polyOrder = Integer.parseInt(polyOrderParam.getExpression());

		
		AbstractDataset dataDS = ((AbstractDataset)data.get(dataset)).clone();
		AbstractDataset xAxisDS = ((AbstractDataset)data.get(xAxis)).clone();
		
		
		AbstractDataset smoothed;
		try {
			smoothed = ApachePolynomial.getPolynomialSmoothed(xAxisDS, dataDS, windowSize, polyOrder);
		} catch (Exception e) {
			throw new DataMessageException("FAiled to get smoothed polynomial",null,e);
		}
		smoothed.setName(dataset+"_smoothed");
		
		result.addList(smoothed.getName(), smoothed);

		return result;
	}

	@Override
	protected String getOperationName() {
		return "Fit 1D data in 2D image";
	}

}
