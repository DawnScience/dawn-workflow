/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dawb.passerelle.common.message.IVariable;

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

}
