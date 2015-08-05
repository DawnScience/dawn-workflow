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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.castor.util.Base64Decoder;
import org.castor.util.Base64Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

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
				return getBean(xml, clazz.getClassLoader());
				
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
	public final String getValueFromBean(Object bean) {
		if (bean==null) return null;
		try {
			final String xml  = getString(bean);
			final String save = new String(Base64Encoder.encode(xml.getBytes("UTF-8")));
			return save;
		} catch (Exception e) {
			logger.error("Cannot write bean "+bean, e);
			return null;
		}
	}

	

	/**
	 * Used externally to the GDA.
	 * 
	 * @param bean
	 * @return the string
	 */
	public static String getString(Object bean) throws Exception {

		final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(bean.getClass().getClassLoader());
			XMLEncoder e = new XMLEncoder(new BufferedOutputStream(stream));
			e.writeObject(bean);
			e.close();
			return stream.toString("UTF-8");
		} finally {
			Thread.currentThread().setContextClassLoader(original);
			stream.close();
		}
	}
	/**
	 * Bean from string using standard java serialization, useful for tables of beans with serialized strings. Used
	 * externally to the GDA.
	 * 
	 * @param xml
	 * @return the bean
	 */
	public static Object getBean(final String xml, final ClassLoader loader) throws Exception {

		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		try {
			Thread.currentThread().setContextClassLoader(loader);
			XMLDecoder d = new XMLDecoder(new BufferedInputStream(stream));
			final Object bean = d.readObject();
			d.close();
			return bean;
		} finally {
			Thread.currentThread().setContextClassLoader(original);
			stream.close();
		}
	}

}
