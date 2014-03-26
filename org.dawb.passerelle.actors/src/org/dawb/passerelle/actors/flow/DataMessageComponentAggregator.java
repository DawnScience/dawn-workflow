package org.dawb.passerelle.actors.flow;

import java.util.ArrayList;
import java.util.Collection;

import org.dawb.passerelle.common.message.MessageUtils;

import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.forkjoin.AggregationStrategy;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;

public class DataMessageComponentAggregator implements AggregationStrategy {

	public ManagedMessage aggregateMessages(ManagedMessage initialMsg,
			ManagedMessage... otherMessages) throws MessageException {
		if(otherMessages!=null) {
			try {
				Collection<DataMessageComponent> dataComps = new ArrayList<DataMessageComponent>();
				for (ManagedMessage branchedMsg : otherMessages) {
					dataComps.add(MessageUtils.coerceMessage(branchedMsg));
				}
				DataMessageComponent mergedDataComp = MessageUtils.mergeAll(dataComps);
				return MessageUtils.getDataMessage(mergedDataComp, initialMsg);
			} catch (Exception e) {
				throw new MessageException(ErrorCode.MSG_CONSTRUCTION_ERROR, "Error merging branched msgs", e);
			}
		} else {
			return initialMsg;
		}
	}

}
