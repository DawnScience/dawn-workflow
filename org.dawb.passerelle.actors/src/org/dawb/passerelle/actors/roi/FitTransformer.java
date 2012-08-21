/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.roi;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer2Port;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * A transformer to complete a fit on data and roi passed in.
 * 
 * 
 * @author gerring
 *
 */
public class FitTransformer extends AbstractDataMessageTransformer2Port {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3929625474027194107L;

	public FitTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		input.setDisplayName("data");
		inputPort2.setDisplayName("region");
	}
	
	/**
	 * This is a two port operation so that maths on pipelines over folders can be done,
	 * it is not inherently one since a+b = b+a (inlike -, / etc.)
	 */
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> port1Cache, List<DataMessageComponent> port2Cache) throws ProcessingException{
		
		final List<IDataset>  data = MessageUtils.getDatasets(port1Cache);
		final List<ROIBase>   rois = MessageUtils.getRois(port2Cache);
		
		return null;
	}

	@Override
	protected String getExtendedInfo() {
		return "Fit data";
	}
	
	@Override
	protected String getOperationName() {
		return "fit";
	}

}
