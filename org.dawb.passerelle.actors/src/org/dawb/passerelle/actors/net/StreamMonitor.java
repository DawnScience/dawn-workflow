/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreamMonitor extends Thread {

	private static Logger logger = LoggerFactory.getLogger(StreamMonitor.class);
	
	// Id which we look for and disconnect once found.
	private volatile String  uniqueId;
	
	// boolean to store when id found
	private ReentrantLock lock;

	// Stream from which to read.
	private InputStreamReader inputStreamReader;
	
	private List<String> scanLocations;

	/** Create a StreamGobbler.
	 *  @param inputStream The stream to read from.
	 *  @param name The name of this StreamReaderThread,
	 *  which is useful for debugging.
	 * @throws UnsupportedEncodingException 
	 */
	StreamMonitor(InputStream inputStream, String name, String charset, String uniqueId) throws UnsupportedEncodingException {
		super(name);
		inputStreamReader = new InputStreamReader(inputStream, charset);
		this.uniqueId = uniqueId;
		this.lock     = new ReentrantLock();
		this.scanLocations = new ArrayList<String>(7);
	}


	/** Read lines from the inputStream and append them to the
	 *  stringBuffer.
	 */
	public synchronized void run() {
		read();
	}
	
	private final Pattern SCAN_OUTPUT = Pattern.compile("Writing data to file\\:(.+)");

	// Read line-by-line from the stream until we get to the end of the stream
	private void read() {
		BufferedReader lineByLineReader = new BufferedReader(inputStreamReader);
		String line = null;
		try {
			lock.lock();

			while ((line=lineByLineReader.readLine())!=null) {
				logger.debug(getName()+"> "+line);
				
				if (uniqueId.equals(line)) {
					break;
				}
				
				try {
					Matcher scanLine = SCAN_OUTPUT.matcher(line);
					if (scanLine.matches()) {
						scanLocations.add(scanLine.group(1));
					}
				} catch (Exception ignored) {
					continue;
				}
			}
			try {
				lineByLineReader.close();
			} catch (IOException e) {
				logger.debug("Error when reading closing stream: "+getName()+e);
			}
			
		} catch (IOException e) {
			logger.debug("Error when reading from stream: "+getName()+e);
			
		} finally {
			lock.unlock();
		}
		
		return;
	}
	
	/**
	 * Blocks until unique id is found.
	 */
	public void block() {
		lock.lock();
		lock.unlock();
		return;
	}


	public List<String> getScanLocations() {
		return scanLocations;
	}
}
