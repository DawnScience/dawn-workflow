package org.acme.actors.fridge;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageSink;
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
public class FridgeLoggingSink extends AbstractDataMessageSink {

	public FridgeLoggingSink(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		passModeParameter.setVisibility(Settable.EXPERT);
		memoryManagementParam.setVisibility(Settable.EXPERT);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1670216347822618376L;

	@Override
	protected void sendCachedData(List<DataMessageComponent> cache) throws ProcessingException {
		
		final DataMessageComponent msg = MessageUtils.mergeAll(cache);
		
		// Hard coded fridge log, could be parameter for configuring
		System.out.println("Fridge temperature below 4 at "+msg.getScalar("fridge_temp"));
		
        try {
        	// To make demo nice
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
 
	}

}