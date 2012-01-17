/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ui;

import org.eclipse.draw2d.Clickable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.isencia.passerelle.workbench.model.editor.ui.editpart.IActorFigureProvider;
import com.isencia.passerelle.workbench.model.editor.ui.figure.ActorFigure;
import com.isencia.passerelle.workbench.model.editor.ui.figure.EllipseActorFigure;

/**
 * Draws a circular node for actor figures
 * @author gerring
 *
 */
public class InterfaceCustomizer implements IActorFigureProvider {

	@Override
	public ActorFigure getActorFigure(String displayName, Image createImage, Clickable[] clickables) {

		final ActorFigure actorFig = new EllipseActorFigure(displayName, createImage, clickables);
		try {
		    actorFig.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		} catch (Throwable t) {
			// Ignored
		}
		return actorFig;
	}

}
