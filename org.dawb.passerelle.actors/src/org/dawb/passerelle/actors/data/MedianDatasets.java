/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.data;

import java.util.ArrayList;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer2Port;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.CollectionStats;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Median taken of data sets pased to this actor.
 * 
 * Actor blocks until all messages recieved.
 * 
 * @author gerring
 *
 */
public class MedianDatasets extends AbstractDataMessageTransformer2Port {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3427246619822801545L;

	public MedianDatasets(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		passModeParameter.setExpression(EXPRESSION_MODE.get(1));
	}
		
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException{
		
		final List<IDataset>  sets1 = MessageUtils.getDatasets(port1Cache);
		final List<IDataset>  sets2 = MessageUtils.getDatasets(port2Cache);
		final List<IDataset>  sets  = new ArrayList<IDataset>(7);
		if (sets1!=null) sets.addAll(sets1);
		if (sets2!=null) sets.addAll(sets2);
		
        try {
    		final AbstractDataset   median = CollectionStats.median(sets);
    		
			final DataMessageComponent ret = new DataMessageComponent();
			ret.setList(median);
	   		setUpstreamValues(ret, port1Cache, port2Cache);
			ret.putScalar("operation_names", MessageUtils.getNames(sets));
			return ret;

			
		} catch (Exception e) {
			throw createDataMessageException("Cannot generate added data sets", e);
		}
		
	}
	
	@Override
	protected String getExtendedInfo() {
		return "Median data sets";
	}
	
	@Override
	protected String getOperationName() {
		return "median";
	}

}
