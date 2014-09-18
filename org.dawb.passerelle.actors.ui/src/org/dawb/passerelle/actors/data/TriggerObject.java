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

import java.io.File;

import org.eclipse.dawnsci.analysis.api.io.SliceObject;

import com.isencia.passerelle.message.ManagedMessage;

public class TriggerObject {

	private int            index;
	private SliceObject    slice;
	private ManagedMessage trigger;
	private File           file;
	
	/**
	 * May be null
	 * @return
	 */
	public ManagedMessage getTrigger() {
		return trigger;
	}
	public void setTrigger(ManagedMessage trigger) {
		this.trigger = trigger;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public SliceObject getSlice() {
		return slice;
	}
	public void setSlice(SliceObject slice) {
		this.slice = slice;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}

}
