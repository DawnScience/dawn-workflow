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
import org.dawb.passerelle.common.parameter.roi.ROIParameter;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.DoubleDataset;
import org.eclipse.january.dataset.InterpolatorUtils;
import org.eclipse.january.dataset.Maths;

import com.isencia.passerelle.actor.ProcessingException;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;

public class RegionSelectAndScale extends AbstractDataMessageTransformer {

	public ROIParameter selectionROI;
	public StringParameter roiName;
	public StringParameter datasetName;
	public StringParameter anglesAxisAdjustName;
	public StringParameter energyAxisAdjustName;
	public StringParameter angles;
	public StringParameter energies;
	public StringParameter photonEnergyNameParam;
	public StringParameter workFunctionNameParam;
	public StringParameter correctionFunctionNameParam;


	public RegionSelectAndScale(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		selectionROI = new ROIParameter(this, "selectionROI");
		registerConfigurableParameter(selectionROI);
		roiName = new StringParameter(this, "roiName");
		registerConfigurableParameter(roiName);
		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		anglesAxisAdjustName = new StringParameter(this, "anglesAxisAdjustName");
		registerConfigurableParameter(anglesAxisAdjustName);
		energyAxisAdjustName = new StringParameter(this, "energyAxisAdjustName");
		registerConfigurableParameter(energyAxisAdjustName);
		angles = new StringParameter(this, "angles");
		registerConfigurableParameter(angles);
		energies = new StringParameter(this, "energies");
		registerConfigurableParameter(energies);
		photonEnergyNameParam = new StringParameter(this, "photonEnergyNameParam");
		registerConfigurableParameter(photonEnergyNameParam);
		workFunctionNameParam = new StringParameter(this, "workFunctionNameParam");
		registerConfigurableParameter(workFunctionNameParam);
		correctionFunctionNameParam = new StringParameter(this, "correctionFunctionNameParam");
		registerConfigurableParameter(correctionFunctionNameParam);
	}
	
	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {
		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		final Map<String, String> scalar = MessageUtils.getScalar(cache);
		Map<String, AFunction> functions = null;
		try {
			functions = MessageUtils.getFunctions(cache);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// get the roi out of the message, name of the roi should be specified
		RectangularROI roi = (RectangularROI) selectionROI.getRoi();
		
		// check the roi list, and see if one exists
		try {
			Map<String, IROI> rois = MessageUtils.getROIs(cache);
			
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
			result.addList(key, DatasetFactory.createFromObject(data.get(key)));
		}
		
		// Normalise the specified dataset
		String name = datasetName.getExpression();
		String angleAdjustName = anglesAxisAdjustName.getExpression();
		String energyAdjustName = energyAxisAdjustName.getExpression();
		String anglesName = angles.getExpression();
		String energiesName = energies.getExpression();
		String photonEnergyName = photonEnergyNameParam.getExpression();
		String workFunctionName = workFunctionNameParam.getExpression();
		
		if (!(data.containsKey(name) && data.containsKey(anglesName) && data.containsKey(energiesName))) {
			throw new IllegalArgumentException("Region Select and Scale does not have the required data inputs");
		}
		
		// Now get the Dataset and axis
		Dataset dataDS = DatasetFactory.createFromObject(data.get(name)).clone();
		Dataset anglesDS = DatasetFactory.createFromObject(data.get(anglesName)).clone();
		Dataset energiesDS = DatasetFactory.createFromObject(data.get(energiesName)).clone();
		
		// now check to see what the offsets are before calculating the conversions
		double angleOffset = 0.0;
		double energyOffset = 0.0;
		double photonEnergy = 0.0;
		double workFunction = 0.0;
		
		String angleOffsetString = scalar.get(angleAdjustName);
		String energyOffsetString = scalar.get(energyAdjustName);
		String photonEnergyString = scalar.get(photonEnergyName);
		String workFunctionString = scalar.get(workFunctionName);
		
		angleOffset = Double.parseDouble(angleOffsetString);
		energyOffset = Double.parseDouble(energyOffsetString);
		photonEnergy = Double.parseDouble(photonEnergyString);
		workFunction = Double.parseDouble(workFunctionString);
		
		// then get the region
		final int yInc = roi.getPoint()[1]<roi.getEndPoint()[1] ? 1 : -1;
		final int xInc = roi.getPoint()[0]<roi.getEndPoint()[0] ? 1 : -1;
		
		Dataset dataRegion = dataDS;
		Dataset energyRegion = energiesDS;
		
		int angleStart = (int) roi.getPoint()[1];
		int energyStart = (int) roi.getPoint()[0];
		int angleStop = (int) roi.getEndPoint()[1];
		int energyStop = (int) roi.getEndPoint()[0];
		
		dataRegion = dataRegion.getSlice(new int[] { angleStart, energyStart },
				new int[] { angleStop, energyStop },
				new int[] {yInc, xInc});
		
		
		energyRegion = energyRegion.getSlice(new int[] {energyStart },
				new int[] {energyStop },
				new int[] {xInc});
		energyRegion = energyRegion.reshape(energyRegion.getShape()[0],1);
		energyRegion = DatasetUtils.tile(energyRegion, dataRegion.getShape()[0]);
		energyRegion = DatasetUtils.transpose(energyRegion);
		
		
		// Correct for initial offsets
		energyRegion.isubtract(energyOffset);
		
		// Now would be a good place to make some of the corrections required
		
		// No calculate the energies
		// TODO could be optimised
		Dataset photonEnergyDS = DatasetFactory.zeros(energyRegion.getShape(), Dataset.FLOAT64).fill(energyRegion.getShape()).imultiply(photonEnergy);
		Dataset workFunctionDS = DatasetFactory.zeros(energyRegion.getShape(), Dataset.FLOAT64).fill(workFunction);
		
		Dataset bindingEnergy = DatasetFactory.zeros(energyRegion.getShape(), Dataset.FLOAT64);
		bindingEnergy.iadd(photonEnergyDS);
		bindingEnergy.isubtract(workFunctionDS);
		//TODO add in the functional part of this calculation
		bindingEnergy.isubtract(energyRegion);
		
		//TODO this is an approximate value, should probably be corrected.
		Dataset k = Maths.sqrt(bindingEnergy).imultiply(0.51168);
		
		// could correct for K now
		double kStep = k.peakToPeak().doubleValue()/(dataRegion.getShape()[1]-1);
		Dataset kAxis = DatasetFactory.createRange(k.min().doubleValue()-(kStep), k.max().doubleValue()+(kStep), kStep, Dataset.FLOAT32);
		result.addList("k_axis", kAxis);
		
		Dataset regrid = InterpolatorUtils.remapAxis(dataRegion, 1, k, kAxis);
		
		// recreate K axis correctly for the process
		
		Dataset newK  = DatasetFactory.createFromObject(DoubleDataset.class, kAxis);
		newK = newK.reshape(newK.getShape()[0],1);
		newK = DatasetUtils.tile(newK, regrid.getShape()[0]);
		newK = DatasetUtils.transpose(newK);
		
		// need to calculate angleRegion here
		Dataset angleRegion = anglesDS;
		angleRegion.isubtract(angleOffset);
		angleRegion = angleRegion.getSlice(new int[] { angleStart },
				new int[] { angleStop },
				new int[] {yInc});
		angleRegion = angleRegion.reshape(angleRegion.getShape()[0],1);
		angleRegion = DatasetUtils.tile(angleRegion, newK.getShape()[1]);
		
		
		// Finally calculate k parallel
		Dataset kParallel = Maths.multiply(newK, Maths.sin(Maths.toRadians(angleRegion)));
		
		// make axis correction to regrid here
		double KPStep = kParallel.peakToPeak().doubleValue()/(dataRegion.getShape()[0]-1);
		Dataset kParaAxis = DatasetFactory.createRange(kParallel.min().doubleValue()-(KPStep), kParallel.max().doubleValue()+(KPStep), KPStep, Dataset.FLOAT32);
		result.addList("k_parallel_axis", kParaAxis);
		
		regrid = InterpolatorUtils.remapAxis(regrid, 0, kParallel, kParaAxis);
			
		// Return the calculated values
		result.addList("region", dataRegion);
		result.addList("k", k);
		result.addList("k_parallel", kParallel);
		result.addList("regrid", regrid);
	
		
		// do the correction and put that into the pipeline., with a name that should be specified.
		return result;
	}

	@Override
	protected String getOperationName() {
		// TODO Auto-generated method stub
		return null;
	}



}
