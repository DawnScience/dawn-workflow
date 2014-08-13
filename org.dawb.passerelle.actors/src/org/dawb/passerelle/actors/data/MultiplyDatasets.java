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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer2Port;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * All inputs to this method are summed. When the 
 * Inputs stop firing data, it dispatches the data, summed, to the output port.
 * 
 * 
 * @author gerring
 *
 */
public class MultiplyDatasets extends AbstractDataMessageTransformer2Port {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3427246619822801545L;

	public MultiplyDatasets(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}
		
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException{
		
		final List<IDataset>  sets1 = MessageUtils.getDatasets(port1Cache);
		final List<IDataset>  sets2 = MessageUtils.getDatasets(port2Cache);
		final Collection<IDataset>  sets  = new HashSet<IDataset>(7);
		if (sets1!=null) sets.addAll(sets1);
		if (sets2!=null) sets.addAll(sets2);
		final Dataset prod = Maths.multiply(sets, isCreateClone());

		try {
			final DataMessageComponent ret = new DataMessageComponent();
			ret.setList(prod);
	   		setUpstreamValues(ret, port1Cache, port2Cache);
			ret.putScalar("operation_names", MessageUtils.getNames(sets));
			return ret;

		} catch (Exception e) {
			throw createDataMessageException("Cannot generate multiplied data sets", e);
		}
		
	}
	
	@Override
	protected String getExtendedInfo() {
		return "Adds data sets";
	}
	@Override
	protected String getOperationName() {
		return "multiply";
	}

}
