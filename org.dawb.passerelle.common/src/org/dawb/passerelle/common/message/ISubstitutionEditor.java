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

import org.eclipse.ui.IEditorPart;

public interface ISubstitutionEditor extends IEditorPart {

	void setPreview(boolean b);

	void revert();

	void setSubstitutionParticipant(SubstitutionParticipant substituteTransformer);

}
