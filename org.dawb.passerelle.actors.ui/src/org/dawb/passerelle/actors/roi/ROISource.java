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

import java.util.ArrayList;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.roi.ROIParameter;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.api.roi.IROI;

import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;

//////////////////////////////////////////////////////////////////////////
//// ROI

/**
 * Sends a scalar message once on each output port.
 */
public class ROISource extends AbstractDataMessageSource {

	
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ROISource.class);

    /**
	 * 
	 */
	private static final long serialVersionUID = 6530803848861756427L;
	
	/** The value produced by this constant source.
	 *  By default, it contains an StringToken with an empty string.  
	 */
	public  ROIParameter     roiParam;
	public  Parameter        nameParam;
	
	/** Construct a constant source with the given container and name.
	 *  Create the <i>value</i> parameter, initialize its value to
	 *  the default value of an IntToken with value 1.
	 *  @param container The container.
	 *  @param name The name of this actor.
	 *  @exception IllegalActionException If the entity cannot be contained
	 *   by the proposed container.
	 *  @exception NameDuplicationException If the container already has an
	 *   actor with this name.
	 */
	public ROISource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
			       
	    nameParam = new StringParameter(this, "Name");
		nameParam.setExpression("roi");
		nameParam.setDisplayName("ROI Name");
		registerConfigurableParameter(nameParam);

		roiParam = new ROIParameter(this, "Region");
		//roiParam.setRoi(new RectangularROI()); // We default it so that something runs.
		registerConfigurableParameter(roiParam);
		
	}
	
	public static ROISource createSource(String name, IROI roi) throws NameDuplicationException, IllegalActionException {
		ROISource source = new ROISource(new CompositeEntity(), name);
		source.roiParam.setRoi(roi);
		return source;
	}


	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		super.attributeChanged(attribute);
	}
	
	protected boolean firedOnce = false;
	
	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (firedOnce) return null; // We only send one ROI from here.
        try {
    		DataMessageComponent despatch = new DataMessageComponent();
    		despatch.addROI(nameParam.getExpression(), roiParam.getRoi());

    		try {
    			return MessageUtils.getDataMessage(despatch, null);
    		} catch (Exception e) {
    			throw createDataMessageException("Cannot set scalar "+nameParam.getExpression(), e);
    		}
        } finally {
        	firedOnce = true;
        }
        
	}	

    /**
	 * @see be.tuple.passerelle.engine.actor.Source#getInfo()
	 */
	protected String getExtendedInfo() {
		return roiParam.getExpression();
	}

	@Override
	public List<IVariable> getOutputVariables() {
		try {
			final String  strName  = ((StringToken) nameParam.getToken()).stringValue();
			final IROI roi      = roiParam.getRoi();
		    final List<IVariable> ret = new ArrayList<IVariable>(1);
		    ret.add(new Variable(strName, VARIABLE_TYPE.ROI, roi, IROI.class));
		    return ret;
		} catch (Exception e) {
			logger.error("Cannot create outputs for "+getName(), e);
		}
		return null;
	}
	
	@Override
	public List<IVariable> getInputVariables() {
        return null;
	}
	
	@Override
	protected boolean mustWaitForTrigger() {
		return false;
	}


}
