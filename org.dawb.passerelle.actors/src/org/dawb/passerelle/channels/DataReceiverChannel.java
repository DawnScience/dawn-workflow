/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.channels;

import com.isencia.message.ChannelException;
import com.isencia.message.ISenderChannel;
import com.isencia.message.ReaderReceiverChannel;
import com.isencia.message.extractor.IMessageExtractor;
/**
 *   DataLoaderReceiverChannel a channel for reading data sets
 *   from arbitrary data files.
 *
 *   @author gerring
 *   @date Aug 25, 2010
 *   @project org.dawb.passerelle.actors
 **/
public class DataReceiverChannel extends ReaderReceiverChannel{


	public DataReceiverChannel(IMessageExtractor extractor) {
		super(extractor);
	}
	
	/**
	 * @see ISenderChannel#open()
	 */
	public void open() throws ChannelException {
		
		setReader(null);
		super.open();

	}
	
	public void close() throws ChannelException {
		
	}
}
