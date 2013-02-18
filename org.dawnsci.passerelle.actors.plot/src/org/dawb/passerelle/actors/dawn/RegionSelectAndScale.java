package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.roi.ROIParameter;

import com.isencia.passerelle.actor.ProcessingException;

import ptolemy.actor.Actor;
import ptolemy.actor.Director;
import ptolemy.actor.Initializable;
import ptolemy.actor.Manager;
import ptolemy.actor.Receiver;
import ptolemy.actor.util.FunctionDependency;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InvalidStateException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.ROIProfile;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

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
		AbstractDataset dataDS = ((AbstractDataset)data.get(name)).clone();
		AbstractDataset anglesDS = ((AbstractDataset)data.get(anglesName)).clone();
		AbstractDataset energiesDS = ((AbstractDataset)data.get(energiesName)).clone();
		
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
		
		AbstractDataset dataRegion = dataDS;
		AbstractDataset angleRegion = anglesDS;
		AbstractDataset energyRegion = energiesDS;
		
		int angleStart = (int) roi.getPoint()[1];
		int energyStart = (int) roi.getPoint()[0];
		int angleStop = (int) roi.getEndPoint()[1];
		int energyStop = (int) roi.getEndPoint()[0];
		
		dataRegion = dataRegion.getSlice(new int[] { angleStart, energyStart },
				new int[] { angleStop, energyStop },
				new int[] {yInc, xInc});
		
		angleRegion = angleRegion.getSlice(new int[] { angleStart },
				new int[] { angleStop },
				new int[] {yInc});
		angleRegion = angleRegion.reshape(angleRegion.getShape()[0],1);
		angleRegion = DatasetUtils.tile(angleRegion, dataRegion.getShape()[1]);
		
		
		energyRegion = energyRegion.getSlice(new int[] {energyStart },
				new int[] {energyStop },
				new int[] {xInc});
		energyRegion = energyRegion.reshape(energyRegion.getShape()[0],1);
		energyRegion = DatasetUtils.tile(energyRegion, dataRegion.getShape()[0]);
		energyRegion = DatasetUtils.transpose(energyRegion);
		
		
		// Correct for initial offsets
		angleRegion.isubtract(angleOffset);
		energyRegion.isubtract(energyOffset);
		
		// No calculate the energies
		DoubleDataset photonEnergyDS = DoubleDataset.ones(energyRegion.getShape()).imultiply(photonEnergy);
		DoubleDataset workFunctionDS = DoubleDataset.ones(energyRegion.getShape()).imultiply(workFunction);
		
		DoubleDataset bindingEnergy = DoubleDataset.ones(energyRegion.getShape()).imultiply(0);
		bindingEnergy.iadd(photonEnergyDS);
		bindingEnergy.isubtract(workFunctionDS);
		//TODO add in the functional part of this calculation
		bindingEnergy.isubtract(energyRegion);
		
		//TODO this is an approximate value, should probably be corrected.
		AbstractDataset k = Maths.sqrt(bindingEnergy).imultiply(0.51168);
		
		// Finally calculate k paralell
		AbstractDataset kParallel = Maths.multiply(k, Maths.sin(Maths.toRadians(angleRegion)));
		
		// Return the calculated values
		result.addList("region", dataRegion);
		result.addList("k", k);
		result.addList("k_parallel", kParallel);
		
		double kStep = k.peakToPeak().doubleValue()/(dataRegion.getShape()[1]-1);
		AbstractDataset kAxis = AbstractDataset.arange(k.min().doubleValue()-(kStep), k.max().doubleValue()+(kStep), kStep, AbstractDataset.FLOAT32);
		result.addList("k_axis", kAxis);
		double KPStep = kParallel.peakToPeak().doubleValue()/(dataRegion.getShape()[0]-1);
		AbstractDataset kParaAxis = AbstractDataset.arange(kParallel.min().doubleValue()-(KPStep), kParallel.max().doubleValue()+(KPStep), KPStep, AbstractDataset.FLOAT32);
		result.addList("k_parallel_axis", kParaAxis);
		
		// do the correction and put that into the pipeline., with a name that should be specified.
		return result;
	}

	@Override
	protected String getOperationName() {
		// TODO Auto-generated method stub
		return null;
	}



}
