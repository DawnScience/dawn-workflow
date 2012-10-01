/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.jmx.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreamGobbler extends Thread {

	private static Logger logger = LoggerFactory.getLogger(StreamGobbler.class);

	private boolean streamLogsToLogging=false;

	// Stream from which to read.
	private InputStreamReader inputStreamReader;

	// Indicator that the stream has been closed.
	private boolean inputStreamReaderClosed = false;

	// StringBuffer to maintain data from configured input stream.
	private List<String> streamDataAsList;

	/** Create a StreamGobbler.
	 *  @param inputStream The stream to read from.
	 *  @param name The name of this StreamReaderThread,
	 *  which is useful for debugging.
	 */
	StreamGobbler(InputStream inputStream, String name) {
		super(name);
		inputStreamReader = new InputStreamReader(inputStream);
		streamDataAsList = new ArrayList<String>();
	}

	/**
	 * @return the streamDataAsStringBuilder
	 */
	public String getStreamDataAsString() {
		final StringBuilder buf = new StringBuilder();
		for (String line : streamDataAsList) {
			buf.append(line);
			buf.append("\n");
		}
		return buf.toString();
	}

	/**
	 * @return the streamDataAsList
	 */
	public List<String> getStreamDataAsList() {
		return streamDataAsList;
	}

	/** Read lines from the inputStream and append them to the
	 *  stringBuffer.
	 */
	public synchronized void run() {
		if (!inputStreamReaderClosed) read();
	}

	// Read line-by-line from the stream until we get to the end of the stream
	private void read() {
		BufferedReader lineByLineReader = new BufferedReader(inputStreamReader);
		String line = null;
		try {
			while ((line=lineByLineReader.readLine())!=null) {
				if (streamLogsToLogging) {
					logger.debug(getName()+"> "+line);
				} else {
					streamDataAsList.add(line);
				}
			}
			readRemainder();
		} catch (IOException e) {
			logger.debug("Error when reading from stream: "+getName()+e);
		}
		try {
			inputStreamReaderClosed = true;
			lineByLineReader.close();
		} catch (IOException e) {
			logger.debug("Error when reading closing stream: "+getName()+e);
		}
		
		return;
	}


	// Read any remaining data from the stream until we get to the end of the stream
	private void readRemainder() throws IOException {
		// We read the data as a char[] instead of using readline()
		// so that we can get strings that do not end in end of
		// line chars.
		char[] chars = new char[80];
		int length; // Number of characters read.

		// Oddly, InputStreamReader.read() will return -1
		// if there is no data present, but the string can still
		// read.
		while (((length = inputStreamReader.read(chars, 0, 80)) != -1)) {
			logger.trace("_read(): Gobbler '" + getName() + "' Ready: "
						+ inputStreamReader.ready() + " Value: '"
						+ String.valueOf(chars, 0, length) + "'");

			if (streamLogsToLogging) {
				logger.debug(getName()+"> "+new String(chars));
			} else {
				streamDataAsList.add(new String(chars));
			}
		}
	}

	public boolean isClosed() {
		return inputStreamReaderClosed;
	}

	public void setStreamLogsToLogging(boolean streamLogsToLogging) {
		this.streamLogsToLogging = streamLogsToLogging;
	}
}
