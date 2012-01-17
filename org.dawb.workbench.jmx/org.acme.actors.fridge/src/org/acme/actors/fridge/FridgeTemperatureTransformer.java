package org.acme.actors.fridge;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

public class FridgeTemperatureTransformer extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8808664164352113469L;

	public FridgeTemperatureTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		passModeParameter.setVisibility(Settable.EXPERT);
		memoryManagementParam.setVisibility(Settable.EXPERT);
		dataSetNaming.setVisibility(Settable.EXPERT);
	}

	@Override
	protected String getOperationName() {
		return "Get fridge temperator";
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
        final DataMessageComponent ret = MessageUtils.mergeAll(cache);
        ret.putScalar("fridge_temp", String.valueOf(getRandomTemperature(5)));
        
        try {
        	// To make demo nice
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
		return ret;
	}

	private double getRandomTemperature(int range) {
		
		final double rand = Math.random();
		return (rand-0.5)*(range*2d);
	}
	
	@Override
	public List<IVariable> getOutputVariables() {
		
		try {
		    final List<IVariable>    ret  = super.getOutputVariables();
		    
		    ret.add(new Variable("fridge_temp", VARIABLE_TYPE.SCALAR, getRandomTemperature(5), Double.class));
		
			return ret;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}


}