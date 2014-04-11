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

import org.eclipse.draw2d.Clickable;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import com.isencia.passerelle.workbench.model.editor.ui.editpart.IActorFigureProvider;
import com.isencia.passerelle.workbench.model.editor.ui.figure.ActorFigure;
import com.isencia.passerelle.workbench.model.editor.ui.figure.EllipseActorFigure;

/**
 * Draws a circular node for actor figures
 * @author gerring
 *
 */
public class MathCustomizer implements IActorFigureProvider {

	public final static Color BACKGROUND_COLOR = new Color(null,255,255,255);
	@Override
	public ActorFigure getActorFigure(String displayName, Image createImage, Clickable[] clickables) {

		final ActorFigure actorFig = new EllipseActorFigure(displayName, createImage, clickables);
		actorFig.setBackgroundColor(BACKGROUND_COLOR);
		return actorFig;
	}

}
