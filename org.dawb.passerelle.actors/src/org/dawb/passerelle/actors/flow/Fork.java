package org.dawb.passerelle.actors.flow;

import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.message.MessageException;


public class Fork extends com.isencia.passerelle.actor.forkjoin.Fork {

	public Fork(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		setAggregationStrategy(new DataMessageComponentAggregator());
	}
	
	@Override
	protected Object cloneScopeMessageBodyContent(Object scopeMessageBodyContent) throws MessageException {
		if(scopeMessageBodyContent instanceof DataMessageComponent) {
			return MessageUtils.copy((DataMessageComponent) scopeMessageBodyContent);
		} else {
			return super.cloneScopeMessageBodyContent(scopeMessageBodyContent);
		}
	}

}
