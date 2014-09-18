/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.message;

import java.util.Arrays;

import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;


/**
 * A class to print data from data sets without reading them in.
 * 
 * It estimates what should be printed and returns it in toString()
 * 
 * @author gerring
 *
 */
public class AbstractDatasetProvider {

	private final int[] shape;
	private final int   dType;
	private String cachedValue;
	
	public AbstractDatasetProvider() {
		this(null, Dataset.FLOAT32);
	}
	public AbstractDatasetProvider(final int[] shape) {
		this(shape, Dataset.FLOAT32);
	}
	public AbstractDatasetProvider(int[] shape, int dType) {
		this.shape = shape;
		this.dType = dType;
	}
	
	public String toString() {
		if (shape==null) {
			return getString();
		}
		
		if (cachedValue==null) {
			final StringBuilder out  = new StringBuilder();
			final StringBuilder lead = new StringBuilder();
			printBlocks(out, lead, 0, new int[getRank()]);
			cachedValue = out.toString();
		}
		return cachedValue;
	}
	
	private String getString() {
		switch(dType) {
		case Dataset.INT16:
			return "1, 2, 3...";
		case Dataset.INT32:
			return "1, 2, 3...";
		case Dataset.INT64:
			return "1, 2, 3...";
		case Dataset.FLOAT32:
			return "1.0, 2.0, 3.0...";
		case Dataset.FLOAT64:
			return "1.0, 2.0, 3.0...";
		default:
			return "1.0, 2.0, 3.0...";
		}
	}
	
	private String getString(int[] values) {
		
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			
			final int value = values[i];
			SWITCH: switch(dType) {
			case Dataset.INT16:
				buf.append(String.valueOf(value));
				break SWITCH;
			case Dataset.INT32:
				buf.append(String.valueOf(value));
				break SWITCH;
			case Dataset.INT64:
				buf.append(String.valueOf(value));
				break SWITCH;
			case Dataset.FLOAT32:
				buf.append(String.valueOf((float)value));
				break SWITCH;
			case Dataset.FLOAT64:
				buf.append(String.valueOf((double)value));
				break SWITCH;
			default:
				buf.append(String.valueOf((double)value));
				break SWITCH;
			}
			
			if (i < (values.length-1)) {
				buf.append(" ,");
			}
		}
		return buf.toString();
	}

	
	private int getRank() {
		return shape!=null ? shape.length : 1;
	}
	
	private final static String SEPARATOR = ",";
	private final static String SPACING = " ";
	private final static String ELLIPSES = "...";
	private final static String NEWLINE = "\n";
	private static final int MAX_SUBBLOCKS = 6;

	/**
	 * recursive method to print blocks
	 */
	private void printBlocks(final StringBuilder out, final StringBuilder lead, final int level, final int[] pos) {
		if (out.length() > 0) {
			String last = out.substring(out.length()-1); 
			if (!last.equals("[")) {
				out.append(lead);
			}
		}
		final int end = getRank() - 1;
		if (level != end) {
			out.append('[');
			int length = shape[level];

			// first sub-block
			pos[level] = 0;
			StringBuilder newlead = new StringBuilder(lead);
			newlead.append(SPACING);
			printBlocks(out, newlead, level + 1, pos);
			if (length < 2) { // escape 
				out.append(']');
				return;
			}

			out.append(SEPARATOR + NEWLINE);
			for (int i = level+1; i < end; i++)
				out.append(NEWLINE);

			// middle sub-blocks
			if (length < MAX_SUBBLOCKS) {
				for (int x = 1; x < length - 1; x++) {
					pos[level] = x;
					printBlocks(out, newlead, level + 1, pos);
					if (end <= level + 1)
						out.append(SEPARATOR + NEWLINE);
					else
						out.append(SEPARATOR + NEWLINE + NEWLINE);
				}
			} else {
				final int excess = length - MAX_SUBBLOCKS;
				int xmax = (length - excess) / 2;
				for (int x = 1; x < xmax; x++) {
					pos[level] = x;
					printBlocks(out, newlead, level + 1, pos);
					if (end <= level + 1)
						out.append(SEPARATOR + NEWLINE);
					else
						out.append(SEPARATOR + NEWLINE + NEWLINE);
				}
				out.append(newlead);
				out.append(ELLIPSES + SEPARATOR + NEWLINE);
				xmax = (length + excess) / 2;
				for (int x = xmax; x < length - 1; x++) {
					pos[level] = x;
					printBlocks(out, newlead, level + 1, pos);
					if (end <= level + 1)
						out.append(SEPARATOR + NEWLINE);
					else
						out.append(SEPARATOR + NEWLINE + NEWLINE);
				}
			}

			// last sub-block
			pos[level] = length - 1;
			printBlocks(out, newlead, level + 1, pos);
			out.append(']');
		} else {
			out.append(makeLine(end, pos));
		}
	}
	
	/**
	 * Limit to strings output via the toString() method
	 */
	private static final int MAX_STRING_LENGTH = 120;

	/**
	 * Make a line of output for last dimension of dataset
	 * @param start
	 * @return line
	 */
	private StringBuilder makeLine(final int end, final int... start) {
		StringBuilder line = new StringBuilder();
		final int[] pos;
		if (end >= start.length) {
			pos = Arrays.copyOf(start, end+1);
		} else {
			pos = start;
		}
		pos[end] = 0;
		line.append('[');
		line.append(getString(pos));

		final int length = shape[end];

		// trim elements printed if length exceed estimate of maximum elements
		int excess = length - MAX_STRING_LENGTH/3; // space + number + separator 
		if (excess > 0) {
			int index = (length - excess)/2;
			for (int y = 1; y < index; y++) {
				line.append(SEPARATOR + SPACING);
				pos[end] = y;
				line.append(getString(pos));
			}
			index = (length + excess)/2;
			for (int y = index; y < length; y++) {
				line.append(SEPARATOR + SPACING);
				pos[end] = y;
				line.append(getString(pos));
			}
		} else {
			for (int y = 1; y < length; y++) {
				line.append(SEPARATOR + SPACING);
				pos[end] = y;
				line.append(getString(pos));
			}
		}
		line.append(']');

		// trim string down to limit
		excess = line.length() - MAX_STRING_LENGTH - ELLIPSES.length() - 1;
		if (excess > 0) {
			int index = line.substring(0, (line.length() - excess) / 2).lastIndexOf(SEPARATOR) + 2;
			StringBuilder out = new StringBuilder(line.subSequence(0, index));
			out.append(ELLIPSES + SEPARATOR);
			index = line.substring((line.length() + excess) / 2).indexOf(SEPARATOR) + (line.length() + excess) / 2 + 1;
			out.append(line.subSequence(index, line.length()));
			return out;
		}

		return line;
	}

}
