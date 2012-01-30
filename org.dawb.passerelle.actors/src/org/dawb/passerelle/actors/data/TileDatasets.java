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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.util.list.ListUtils;
import org.dawb.common.util.list.PrimitiveArrayEncoder;
import org.dawb.common.util.text.NumberUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IntegerDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * A tile designed to work in a similar way to the numpy tile:
 * http://docs.scipy.org/doc/numpy/reference/generated/numpy.tile.html
 * 
 * This uses Peter Changs API
 * 
 * 
 * @author gerring
 *
 */
public class TileDatasets extends AbstractDataMessageTransformer {


	/**
	 * 
	 */
	private static final long serialVersionUID = -6239616184296978488L;
	
	/**
	 * 
	 */
	private StringParameter repetitions;
	
	/**
	 * The data names passed into the actor that should be tiled. If left bank ALL
	 * data will be tiled.
	 */
	private StringParameter dataNames;


	public TileDatasets(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		/**
		 * Comma separated integer values for number of repetitions in each dimension.
		 */
        this.repetitions = new StringParameter(this, "Tile repetitions");
        registerConfigurableParameter(repetitions);
        
		/**
		 * Comma separated string values for variables to tile
		 */
        this.dataNames = new StringParameter(this, "Data Names");
        registerConfigurableParameter(dataNames);

        
        memoryManagementParam.setVisibility(Settable.NONE);
        dataSetNaming.setVisibility(Settable.NONE);
	}
	

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {

		if (repetitions.getExpression()==null || "".equals(repetitions.getExpression())) throw createDataMessageException("You must set the "+repetitions.getDisplayName()+" attribute!", null); 
		
		final Map<String,Serializable> tiled = new HashMap<String,Serializable>(31);
		final int[] tileSize = PrimitiveArrayEncoder.getIntArrayNoBrackets(repetitions.getExpression());
		
		if (dataNames.getExpression()==null || "".equals(dataNames.getExpression())) {
			for (DataMessageComponent comp : cache) {
				final Map<String,Serializable> sets = comp.getList();
				if (sets!=null) for (String name : sets.keySet()) {
					final Object value = sets.get(name);
					if (!(value instanceof IDataset)) continue;
					final IDataset   set = (IDataset)value;
					final IDataset  tile = DatasetUtils.tile(set, tileSize);
					tiled.put(name, tile);
				}
			}
		} else { // Loop over names they would like tiled, even if scalar.
			final Map<String, String>     scalar = MessageUtils.getScalar(cache);
			final Map<String, Serializable> list = MessageUtils.getList(cache);
			final List<String>            names  = ListUtils.getList(dataNames.getExpression());
			for (String name : names) {
				IDataset set = list!=null ? (IDataset)list.get(name) : null;
				if (set==null && scalar!=null && scalar.containsKey(name)) {
					final Number num = (Number)NumberUtils.getNumberIfParses(scalar.get(name));
					// Intentionally throws cast exception if is not number!
					// Use fill here, as much faster than tile...
					if (num instanceof Integer) {
						set = new IntegerDataset(tileSize);
					} else {
						set = new DoubleDataset(tileSize);
					}
				    ((AbstractDataset)set).fill(num);
					tiled.put(name, set);
                    continue;
				}
				
				if (set==null) createDataMessageException("Cannot tile variable '"+name+"'!", null);
				
				final IDataset  tile = DatasetUtils.tile(set, tileSize);
				tiled.put(name, tile);

			}
		}
		
        try {
    		final DataMessageComponent ret = new DataMessageComponent();
    		ret.setMeta(MessageUtils.getMeta(cache));
    		ret.addScalar(MessageUtils.getScalar(cache));
			ret.setList(tiled);
			ret.putScalar("operation_names", MessageUtils.getNames(tiled.values()));
			return ret;
			
		} catch (Exception e) {
			throw createDataMessageException("Tiling data sets", e);
		}

	}


	@Override
	protected String getExtendedInfo() {
		return "Tile data sets";
	}
	
	@Override
	protected String getOperationName() {
		return "tile";
	}
}
