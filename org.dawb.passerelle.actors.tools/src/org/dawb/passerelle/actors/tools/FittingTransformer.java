package org.dawb.passerelle.actors.tools;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;

public class FittingTransformer extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9207419832558630927L;

	
	public FittingTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		return null;
	}

	@Override
	protected String getOperationName() {
		return null;
	}

}
