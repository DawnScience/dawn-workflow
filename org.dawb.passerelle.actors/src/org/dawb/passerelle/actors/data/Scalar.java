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

import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.gda.doe.DOEUtils;
import uk.ac.gda.richbeans.components.cell.FieldComponentCellEditor;
import uk.ac.gda.richbeans.components.scalebox.RangeBox;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

//////////////////////////////////////////////////////////////////////////
//// Scalar

/**
 * Sends a scalar message once on each output port.
 */
public class Scalar extends AbstractDataMessageSource {

	
	private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Scalar.class);

    /**
	 * 
	 */
	private static final long serialVersionUID = 6530803848861756427L;
	
	/** The value produced by this constant source.
	 *  By default, it contains an StringToken with an empty string.  
	 */
	public  Parameter        valueParam,nameParam;
	private String           strName, strValue;
	private List<? extends Number> rangeQueue;
	
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
	public Scalar(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
			       
	    nameParam = new StringParameter(this, "Name");
		nameParam.setExpression("x");
		nameParam.setDisplayName("Scalar Name");
		registerConfigurableParameter(nameParam);

		valueParam = new RangeParameter(this, "Value");
		valueParam.setExpression("1");
		valueParam.setDisplayName("Scalar Value");
		registerConfigurableParameter(valueParam);
		
	}

	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == nameParam) {
			strName = nameParam.getExpression();
		}else if (attribute == valueParam) {
			strValue = valueParam.getExpression();
		}
		super.attributeChanged(attribute);
	}
	
	@Override
	protected void doInitialize() throws InitializationException {
	
		super.doInitialize();
		
		rangeQueue = DOEUtils.expand(strValue);
	}

	public boolean hasNoMoreMessages() {
	    if (rangeQueue == null)   return true;
        return rangeQueue.isEmpty() && super.hasNoMoreMessages();
    }
	
	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {

        if (rangeQueue==null)     return null;
        if (rangeQueue.isEmpty()) return null;
        
        final Number num = rangeQueue.remove(0);
        
		DataMessageComponent despatch = new DataMessageComponent();
		despatch.putScalar(strName, num.toString());

		try {
			return MessageUtils.getDataMessage(despatch);
		} catch (Exception e) {
			throw createDataMessageException("Cannot set scalar "+strName, e);
		}
	}	

    /**
	 * @see be.tuple.passerelle.engine.actor.Source#getInfo()
	 */
	protected String getExtendedInfo() {
		return valueParam.getExpression();
	}

	@Override
	public List<IVariable> getOutputVariables() {
		try {
			final String strName  = ((StringToken) nameParam.getToken()).stringValue();
			final String strValue = ((StringToken) valueParam.getToken()).stringValue();
		    final List<IVariable> ret = new ArrayList<IVariable>(1);
		    ret.add(new Variable(strName, VARIABLE_TYPE.SCALAR, strValue, String.class));
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
	
	
	private static class RangeParameter extends StringParameter implements CellEditorAttribute {
		public RangeParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}
		@Override
		public CellEditor createCellEditor(Control control) {
			try {
				return new FieldComponentCellEditor((Composite)control, RangeBox.class.getName());
			} catch (ClassNotFoundException e) {
				logger.error("Cannot create editor for field "+this, e);
				return null;
			}
		}
		@Override
		public String getRendererText() {
			return null;
		}	
	}

	@Override
	protected boolean mustWaitForTrigger() {
		return false;
	}

}
