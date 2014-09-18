/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;

public class DatWriter {

	private Map<String,String>   meta;
	private File                 file;
	private List<IDataset>       data;
	private String               dataFormat = "#0.0000";
	private boolean              isWriteIndex = true;
	
	public Map<String, String> getMeta() {
		return meta;
	}
	public void setMeta(Map<String, String> scalar) {
		this.meta = scalar;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public List<IDataset> getData() {
		return data;
	}
	public void addData(IDataset d) {
		if (data==null) data = new ArrayList<IDataset>(7);
		this.data.add(d);
	}
	public void clear() {
		if (data!=null) data.clear();
	}
	
	/**
	 * Writes the dat file from the current meta (if set)
	 * file (must be set) and data (must be set)
	 * @throws Exception
	 */
	public void write() throws Exception {
		
		final int size = data.get(0).getSize();
		for (IDataset d : data) {
			if (d.getShape().length!=1) throw new Exception("Currrently DatWriter only supports 1D datasets!");
			if (d.getSize()!=size) throw new Exception("All the data sets for writing should be size "+size);
		}
		
		if (file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		file.createNewFile();
		
		final DecimalFormat format = new DecimalFormat(getDataFormat());
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		try {
			
			if (meta!=null) for (String key : meta.keySet()) {
				writer.write("# ");
				writer.write(key);
				writer.write(" = ");
				writer.write(meta.get(key));
				writer.newLine();
			}
			
			final StringBuilder buf = new StringBuilder();
			buf.append("# ");
			if (isWriteIndex) {
				buf.append("Index");
				buf.append("   \t");
			}
			for (IDataset d : data) {
				buf.append(d.getName());
				buf.append("   \t");
			}			
			writer.write(buf.toString());
			writer.newLine();
			
			for (int i = 0; i < size; i++) {
				
				writer.write(" ");
				if (isWriteIndex) {
					writer.write(String.valueOf(i));
					writer.write("   \t");
				}
				for (IDataset d : data) {
					writer.write(format.format(d.getDouble(i)));
					writer.write("   \t");
				}
				writer.newLine();
			}
			
		} finally {
			writer.close();
		}
		
	}
	public String getDataFormat() {
		return dataFormat;
	}
	public void setDataFormat(String dataFormat) {
		this.dataFormat = dataFormat;
	}
	public boolean isWriteIndex() {
		return isWriteIndex;
	}
	public void setWriteIndex(boolean isWriteIndex) {
		this.isWriteIndex = isWriteIndex;
	}
}
