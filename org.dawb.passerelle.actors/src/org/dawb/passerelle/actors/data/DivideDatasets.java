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

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Divides datasets, the sum of inputs to numerator with the
 * sum of inputs to denominator.
 * 
 * 
 * @author gerring
 *
 */
public class DivideDatasets extends AbstractDataMessageTransformer2Port {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3427246619822801545L;

	public DivideDatasets(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException {

		// At least one of the sets must be floating point 
		// or we get integer math.
		final List<IDataset>  sets1 = MessageUtils.getDatasets(port1Cache);
		AbstractDataset       a     = Maths.add(sets1, isCreateClone());
		if (a.getDtype()!=AbstractDataset.FLOAT32 && 
			a.getDtype()!=AbstractDataset.FLOAT64) {
		    
			final String name = a.getName();
			a   = DatasetUtils.cast(a, getFloatType(a));
			a.setName(name);
		}
		
		final List<IDataset>  sets2 = MessageUtils.getDatasets(port2Cache);
		final AbstractDataset b     = Maths.add(sets2, true);
		
		// We use an in place divide, it is changing the original
		// data but more efficient on memory.
		final AbstractDataset res = isCreateClone()
		                          ? Maths.divide(a, b)
		                          : a.idivide(b);
		                          
		res.setName(a.getName()+"/"+b.getName());
		
		final DataMessageComponent ret = new DataMessageComponent();
		ret.setList(res);
		
		// Currently we use just port1Cache for provenance
   		setUpstreamValues(ret, port1Cache, port2Cache);
		ret.putScalar("operation_names", MessageUtils.getNames(a,b));
		
		return ret;
	}

	private int getFloatType(AbstractDataset b) {
		final int type = b.getDtype();
		
		switch (type) {
		case AbstractDataset.INT8:
			return AbstractDataset.FLOAT32;
		case AbstractDataset.INT16:
			return AbstractDataset.FLOAT32;
		case AbstractDataset.INT32:
			return AbstractDataset.FLOAT32;
		case AbstractDataset.INT64:
			return AbstractDataset.FLOAT64;
		}
		return AbstractDataset.FLOAT32;
	}

	@Override
	protected String getExtendedInfo() {
		return "Divides the sum of data sets in Port1 with the sum of data sets in Port2";
	}

	@Override
	protected String getOperationName() {
		return "divide";
	}

}
