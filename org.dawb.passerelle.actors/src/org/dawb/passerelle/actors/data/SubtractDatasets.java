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

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer2Port;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.Maths;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Divides datasets, the sum of inputs to numerator with the
 * sum of inputs to denominator.
 * 
 * 
 * @author gerring
 *
 */
public class SubtractDatasets extends AbstractDataMessageTransformer2Port {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3427246619822801545L;

	public SubtractDatasets(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException {

		final List<IDataset>  sets1 = MessageUtils.getDatasets(port1Cache);
		final List<IDataset>  sets2 = MessageUtils.getDatasets(port2Cache);
		final Dataset a   = Maths.add(sets1, isCreateClone());
		final Dataset b   = Maths.add(sets2, true);
		final Dataset res = isCreateClone()
		                          ? Maths.subtract(a, b)
		                          : a.isubtract(b);
		
        try {
    		final DataMessageComponent ret = new DataMessageComponent();
    		ret.setList(res);
    		
    		// Currently we use just port1Cache for provenance
    		setUpstreamValues(ret, port1Cache, port2Cache);
			ret.putScalar("operation_names", MessageUtils.getNames(a,b));
   		
    		return ret;
			
		} catch (Exception e) {
			throw createDataMessageException("Cannot generate added data sets", e);
		}
	}

	@Override
	protected String getExtendedInfo() {
		return "Divides the sum of data sets in Port1 with the sum of data sets in Port2";
	}

	@Override
	protected String getOperationName() {
		return "subtract";
	}

}
