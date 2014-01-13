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


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VariablePainter implements IPainter, PaintListener {

	private static Logger logger = LoggerFactory.getLogger(VariablePainter.class);
	
	/** The source viewer this painter is associated with */
	private ISourceViewer fSourceViewer;
	/** The viewer's widget */
	private StyledText fTextWidget;
	/** The color in which to highlight the peer character */
	private Color backColor,selColor,blackColor;
	/** The paint position manager */
	private IPaintPositionManager fPaintPositionManager;
	/** The strategy for finding matching characters */
	private VariableCharacterMatcher fMatcher;
	/** The position tracking the matching characters */
	private List<VariablePosition> fPairPositions;
	/** The anchor indicating whether the character is left or right of the caret */

	/**
	 * Creates a new MatchingCharacterPainter for the given source viewer using the given character
	 * pair matcher. The character matcher is not adopted by this painter. Thus, it is not disposed.
	 * However, this painter requires exclusive access to the given pair matcher.
	 *
	 * @param sourceViewer the source viewer
	 * @param matcher the character pair matcher
	 */
	public VariablePainter(ISourceViewer sourceViewer, VariableCharacterMatcher matcher) {
		fSourceViewer= sourceViewer;
		fMatcher= matcher;
		fTextWidget= sourceViewer.getTextWidget();
		this.fPairPositions = new ArrayList<VariablePosition>(7);
		
		selColor   = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		blackColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
		backColor  = new Color(Display.getCurrent(), 230, 230, 228);

	}


	/*
	 * @see org.eclipse.jface.text.IPainter#dispose()
	 */
	public void dispose() {
		if (fMatcher != null) {
			fMatcher.clear();
			fMatcher= null;
		}

		selColor= null;
		blackColor=null;
		backColor.dispose();
		backColor= null;
		fTextWidget= null;
	}

	/*
	 * @see org.eclipse.jface.text.IPainter#deactivate(boolean)
	 */
	public void deactivate(boolean redraw) {
		
		fTextWidget.removePaintListener(this);
		if (fPaintPositionManager != null)
			for (Position fPairPosition : fPairPositions) {
				fPaintPositionManager.unmanagePosition(fPairPosition);
				fPairPosition.delete();
			}
		if (redraw) handleDrawRequest(null);

	}

	/*
	 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	 */
	public void paintControl(PaintEvent event) {
		if (fTextWidget != null)
			handleDrawRequest(event.gc);
	}

	/**
	 * Handles a redraw request.
	 *
	 * @param gc the GC to draw into.
	 */
	private void handleDrawRequest(GC gc) {

		for (VariablePosition fPairPosition : fPairPositions) {
			
			if (fPairPosition.isDeleted) continue;

			int offset= fPairPosition.getOffset();
			int length= fPairPosition.getLength();
			if (length < 1) return;

			if (fSourceViewer instanceof ITextViewerExtension5) {
				ITextViewerExtension5 extension= (ITextViewerExtension5) fSourceViewer;
				IRegion widgetRange= extension.modelRange2WidgetRange(new Region(offset, length));
				if (widgetRange == null) return;

				try {
					// don't draw if the pair position is really hidden and widgetRange just
					// marks the coverage around it.
					IDocument doc= fSourceViewer.getDocument();
					int startLine= doc.getLineOfOffset(offset);
					int endLine= doc.getLineOfOffset(offset + length);
					if (extension.modelLine2WidgetLine(startLine) == -1 || extension.modelLine2WidgetLine(endLine) == -1)
						return;
				} catch (BadLocationException e) {
					return;
				}

				offset= widgetRange.getOffset();
				length= widgetRange.getLength();

			// Bodge warning, block same as above, with different
		    // Object, TODO replace with wrapper design.
			} else if (fSourceViewer instanceof SourceViewer) {
				    SourceViewer extension= (SourceViewer) fSourceViewer;
					IRegion widgetRange= extension.modelRange2WidgetRange(new Region(offset, length));
					if (widgetRange == null) return;

					try {
						// don't draw if the pair position is really hidden and widgetRange just
						// marks the coverage around it.
						IDocument doc= fSourceViewer.getDocument();
						int startLine= doc.getLineOfOffset(offset);
						int endLine= doc.getLineOfOffset(offset + length);
						if (extension.modelLine2WidgetLine(startLine) == -1 || extension.modelLine2WidgetLine(endLine) == -1)
							return;
					} catch (BadLocationException e) {
						return;
					}

					offset= widgetRange.getOffset();
					length= widgetRange.getLength();
			} else {
				IRegion region= fSourceViewer.getVisibleRegion();
				if (region.getOffset() > offset || region.getOffset() + region.getLength() < offset + length)
					return;
				offset -= region.getOffset();
				length  = region.getLength();
			}

			draw(gc, offset, length, fPairPosition.isSelected() ? selColor : blackColor);
		}
	}

	/**
	 * Highlights the given widget				deactivate(true);
				return;
 region.
	 *
	 * @param gc the GC to draw into
	 * @param offset the offset of the widget region
	 * @param length the length of the widget region
	 */
	private void draw(GC gc, int offset, int length, final Color fore) {
		
		if (gc != null) {

			Rectangle bounds;
			if (length > 0)
				bounds= fTextWidget.getTextBounds(offset, offset + length - 1);
			else {
				Point loc= fTextWidget.getLocationAtOffset(offset);
				bounds= new Rectangle(loc.x, loc.y, 1, fTextWidget.getLineHeight(offset));
			}

			gc.setBackground(backColor);

			// draw box around line segment
			gc.fillRectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
            
			gc.setForeground(fore);
			
			gc.drawText(fTextWidget.getText(offset, offset + length - 1), bounds.x, bounds.y);
            
			// draw box around character area
//			int widgetBaseline= fTextWidget.getBaseline();
//			FontMetrics fm= gc.getFontMetrics();
//			int fontBaseline= fm.getAscent() + fm.getLeading();
//			int fontBias= widgetBaseline - fontBaseline;

//			gc.drawRectangle(left.x, left.y + fontBias, right.x - left.x - 1, fm.getHeight() - 1);

		} else {
			fTextWidget.redrawRange(offset, length, true);
		}
	}

	/*
	 * @see org.eclipse.jface.text.IPainter#paint(int)
	 */
	public void paint(int reason) {

		IDocument document= fSourceViewer.getDocument();
		if (document == null) {
			deactivate(false);
			return;
		}

		Point selection= fSourceViewer.getSelectedRange();
		if (selection.y > 0) {
			deactivate(true);
			return;
		}

		IRegion[] matches=null;
		try {
			matches= fMatcher.match(document);
			if (matches == null) {
				deactivate(true);
				return;
			}
		} catch (BadLocationException ne) {
			logger.error("Cannot match variables", ne);
			deactivate(true);
			return;
		}


		fTextWidget.addPaintListener(this);

		if (IPainter.CONFIGURATION == reason) {

			// redraw current highlighting
			handleDrawRequest(null);

		} else if (matches.length>-1) {

			// Remove positions
			if (matches.length<fPairPositions.size()) {
				while (matches.length<fPairPositions.size()) {
					final VariablePosition pos = fPairPositions.remove(fPairPositions.size()-1);
					fPaintPositionManager.unmanagePosition(pos);
					pos.isDeleted = true;
				}
			} else if (matches.length>fPairPositions.size()) {
				for (int i = fPairPositions.size(); i < matches.length; i++) {
					fPairPositions.add(new VariablePosition(0,0));
					fPaintPositionManager.managePosition(fPairPositions.get(i));
				}
			}

			for (int i = 0; i < matches.length; i++) {

				final IRegion  pair          = matches[i];
				final VariablePosition fPairPosition = fPairPositions.get(i);

				if (pair instanceof VariableRegion) {
					fPairPosition.setVariableType(((VariableRegion)pair).getVariableType());
				}
				
				if (pair.getOffset() != fPairPosition.getOffset() ||
				    pair.getLength() != fPairPosition.getLength()) {

					// otherwise only do something if position is different

					// remove old highlighting
					// update position
					fPairPosition.isDeleted= false;
					fPairPosition.offset= pair.getOffset();
					fPairPosition.length= pair.getLength();

				} else {

					fPairPosition.isDeleted= false;
					fPairPosition.offset= pair.getOffset();
					fPairPosition.length= pair.getLength();

				}
			}

			handleDrawRequest(null);

		}
	}

	
     /*
	 * @see org.eclipse.jface.text.IPainter#setPositionManager(org.eclipse.jface.text.IPaintPositionManager)
	 */
	public void setPositionManager(IPaintPositionManager manager) {
		fPaintPositionManager= manager;
	}
}
