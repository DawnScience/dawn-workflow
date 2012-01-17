/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.extractors;

import java.io.File;
import java.io.Reader;

import com.isencia.message.extractor.IMessageExtractor;

/**
 *   DataExtractor
 *
 *   @author gerring
 *   @date Aug 25, 2010
 *   @project org.dawb.passerelle.actors
 **/
public class DataExtractor implements IMessageExtractor {

	private final File file;
	
	public DataExtractor(File file) {
		this.file = file;
	}

	@Override
	public IMessageExtractor cloneExtractor() {
		return new DataExtractor(file);
	}

	@Override
	public void close() {
		// Will close 

	}

	@Override
	public Object getMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void open(Reader reader) {
		// TODO Auto-generated method stub

	}

}
