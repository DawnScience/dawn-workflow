/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.ui.views.ActorValueObject.ActorValueDataType;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;

class ActorValueUtils {

	/**
	 * 
	 * @param in
	 * @param out
	 * @return
	 */
	public static List<ActorValueObject> getTableObjects(List<IVariable> in,
			                                             List<IVariable> out) {
		
		in  = ActorValueUtils.removeDuplicatedNames(in);
		out = ActorValueUtils.removeDuplicatedNames(out);
		final List<ActorValueObject> ret = new ArrayList<ActorValueObject>(7);
		
		final int size = Math.max(in!=null?in.size():0, out!=null?out.size():0);
		if (size<1) return ret;
		
		for (int i = 0; i < size; i++) {
			final IVariable input  = (in!=null && in.size()>i)   ? in.get(i)  : null;
			final IVariable output = (out!=null && out.size()>i) ? out.get(i) : null;
			final ActorValueObject ob = new ActorValueObject();
			ob.setInputName(input!=null?input.getVariableName():null);
			ob.setInputValue(input!=null?input.getExampleValue():null);
			ob.setOutputName(output!=null?output.getVariableName():null);
			ob.setOutputValue(output!=null?output.getExampleValue():null);
			if (!ret.contains(ob)) ret.add(ob);
		}
		
		return ret;
	}

	private static List<IVariable> removeDuplicatedNames(final List<IVariable> list) {
		
		if (list==null) return null;
		
		// Horrible On^2 loop because we do not want to change equals and
		// hashcode of Variable just for this.
		final List<IVariable> found =  new ArrayList<IVariable>(7);
		MAIN: for (Iterator<IVariable> it = list.iterator(); it.hasNext();) {
			final IVariable iv = it.next();
			for (IVariable existing : found) {
				if (existing.getVariableName().equals(iv.getVariableName())) {
					continue MAIN;
				}
			}
			found.add(iv);
		}
		return found;
	}

	public static List<ActorValueObject> getTableObjects(UserDebugBean bean) {
		
		final int size = bean.getDataSize(); // The max of inputSize , outputSize.
		if (size<1) return Collections.emptyList();
		
		List<ActorValueObject> ret = new ArrayList<ActorValueObject>(size);
		
		final List<Entry<String, ?>> inputs  = bean.getInputs();
		final List<Entry<String, ?>> outputs = bean.getOutputs();
		
		for (int i = 0; i < size; i++) {
			ActorValueObject val = new ActorValueObject();
			Entry<String,?> entry = getEntry(i, inputs);
			if (entry!=null) {
				Object value = entry.getValue();
				val.setInDataType(getType(value));
				val.setInputName(entry.getKey());
				val.setInputValue(value);
			}
			entry = getEntry(i, outputs);
			if (entry!=null) {
				Object value = entry.getValue();
				val.setOutputName(entry.getKey());
				val.setOutDataType(getType(value));
				val.setOutputValue(value);
			}
			ret.add(val);
		}
		
		return ret;
	}

	private static ActorValueDataType getType(Object value) {
		if (value instanceof IDataset || value instanceof ILazyDataset) {
			return ActorValueDataType.LIST;
		} else if (value instanceof IROI) {
			return ActorValueDataType.ROI;
		} else {
			return ActorValueDataType.SCALAR;
		}
	}

	private static Entry<String, ?> getEntry(int                    index, 
			                                 List<Entry<String, ?>> data) {
		
		try {
			return data.get(index);
		} catch (Throwable ignored) {
			return null;
		}
	}

}
