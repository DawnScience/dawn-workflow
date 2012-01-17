/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.parameter;

import org.castor.core.util.Base64Decoder;
import org.castor.core.util.Base64Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.gda.richbeans.beans.BeanUI;

import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

public abstract class CellEditorParameter extends StringParameter implements CellEditorAttribute{

	private static Logger logger = LoggerFactory.getLogger(CellEditorParameter.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -5465322994181942257L;

	public CellEditorParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	
	public final Object getBeanFromValue(final Class<? extends Object> clazz) {
		
		try {
			if (getExpression()==null || "".equals(getExpression())) return clazz.newInstance();
			try {
				String xml = getXML();				
				return BeanUI.getBean(xml, clazz.getClassLoader());
				
			} catch (Exception e) {
				logger.error("Cannot read bean "+super.getExpression(), e);
				return clazz.newInstance();
			}
		} catch (Exception ne) {
			logger.error("There is no null argument constructor in "+clazz.getName(), ne);
			return new Object();
		}
	}	
	
	public String getXML() {
		
		String xml = getExpression();				
		if (!xml.startsWith("<?xml")) {
			try {
				final byte[] bytes = Base64Decoder.decode(getExpression());
				xml = new String(bytes, "UTF-8");
			} catch (Throwable ne) {
				xml = getExpression();
			}
		}
		return xml;
	}


	/**
	 * returns the string which is to be saved in the parameter.
	 * @param bean
	 * @return
	 */
	protected final String getValueFromBean(Object bean) {
		if (bean==null) return null;
		try {
			final String xml  = BeanUI.getString(bean);
			final String save = new String(Base64Encoder.encode(xml.getBytes("UTF-8")));
			return save;
		} catch (Exception e) {
			logger.error("Cannot write bean "+bean, e);
			return null;
		}
	}

}
