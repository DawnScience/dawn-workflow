/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.ui.editors;

import java.util.Comparator;

import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.XPathVariable;

public class XPathComparitor implements Comparator<IVariable> {

	private XPathParticipant actor;

	public XPathComparitor(XPathParticipant actor) {
		this.actor = actor;
	}

	@Override
	public int compare(IVariable o1, IVariable o2) {
		if (o1 instanceof XPathVariable && !(o2 instanceof XPathVariable)) {
			return -1;
		}
		if (o2 instanceof XPathVariable && !(o1 instanceof XPathVariable)) {
			return 1;
		}
		
		if (o1 instanceof XPathVariable && o2 instanceof XPathVariable) {
			XPathVariable x1 = (XPathVariable)o1;
			XPathVariable x2 = (XPathVariable)o2;
			if (actor.isUpstreamVariable(x1.getVariableName()) && !actor.isUpstreamVariable(x2.getVariableName())) {
				return -1;
			}
			if (!actor.isUpstreamVariable(x1.getVariableName()) && actor.isUpstreamVariable(x2.getVariableName())) {
				return 1;
			}
		}
		return o1.getVariableName().compareTo(o2.getVariableName());
	}

}
