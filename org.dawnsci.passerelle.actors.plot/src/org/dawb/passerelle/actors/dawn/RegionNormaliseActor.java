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
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.roi.ROIParameter;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

import com.isencia.passerelle.actor.ProcessingException;

public class RegionNormaliseActor extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 813882139346261410L;
	public ROIParameter normalisationROI;
	public StringParameter roiName;
	public StringParameter datasetName;

	public RegionNormaliseActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		normalisationROI = new ROIParameter(this, "NormalisationROI");
		registerConfigurableParameter(normalisationROI);
		roiName = new StringParameter(this, "roiName");
		registerConfigurableParameter(roiName);
		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		
		// get the roi out of the message, name of the roi should be specified
		RectangularROI roi = (RectangularROI) normalisationROI.getRoi();

		// check the roi list, and see if one exists
		try {
			Map<String, ROIBase> rois = MessageUtils.getROIs(cache);

			if(rois.containsKey(roiName.getExpression())) {
				if (rois.get(roiName.getExpression()) instanceof RectangularROI) {
					roi = (RectangularROI) rois.get(roiName.getExpression());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();
		
		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, (AbstractDataset) data.get(key));
		}

		// Normalise the specified dataset
		String name = datasetName.getExpression();
		if (data.containsKey(name)) {
			AbstractDataset ds = ((AbstractDataset)data.get(name)).clone();
			AbstractDataset[] profiles = ROIProfile.box(ds, roi);
			AbstractDataset tile = profiles[1].reshape(profiles[1].getShape()[0],1);
			double width = roi.getLengths()[0];
			tile.idivide(width);
			AbstractDataset correction = DatasetUtils.tile(tile, ds.getShape()[1]);
			
			result.addList(ds.getName()+"_norm", ds.idivide(correction));
			result.addList(ds.getName()+"_correction_map", correction);
		}
		
		// do the correction and put that into the pipeline., with a name that should be specified.
		return result;
	}

	@Override
	protected String getOperationName() {
		return "Normalise by region";
	}

}
