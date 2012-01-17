package org.acme.actors.fridge;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * You must input the ambient and current fridge temperature into the 
 * cycle algorithm
 * 
 * @author fcp94556
 *
 */
public class CoolingTransformer extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8808664164352113469L;

	public CoolingTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		passModeParameter.setVisibility(Settable.EXPERT);
		memoryManagementParam.setVisibility(Settable.EXPERT);
		dataSetNaming.setVisibility(Settable.EXPERT);
	}

	@Override
	protected String getOperationName() {
		return "Run cooling cycle.";
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
        final DataMessageComponent ret = MessageUtils.mergeAll(cache);
        ret.putScalar("cooling_cycle_run", String.valueOf(System.currentTimeMillis()));
        
        try {
        	// To make demo nice
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
 
		return ret;
	}

}