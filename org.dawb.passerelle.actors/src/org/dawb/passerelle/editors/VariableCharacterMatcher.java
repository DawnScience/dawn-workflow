/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.editors;

import java.util.ArrayList;
import java.util.List;

import org.dawb.passerelle.common.message.IVariable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableCharacterMatcher implements ISelectionChangedListener {

	private static final Logger logger = LoggerFactory.getLogger(VariableCharacterMatcher.class);
	
	private IVariable    selectedVariable;
	private SourceViewer textViewer;
	
	public VariableCharacterMatcher(SourceViewer textViewer) {
		this.textViewer = textViewer;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		
		final StructuredSelection sel = (StructuredSelection)event.getSelection();
		if (!(sel.getFirstElement() instanceof IVariable)) return;
		final IVariable           var = (IVariable)sel.getFirstElement();
		
		final TableViewer tableViewer = (TableViewer)event.getSource();
		if (!tableViewer.getTable().isEnabled()) {
			this.selectedVariable = null;
		} else {
		    this.selectedVariable = var;
		}
		
		final int off = textViewer.getTextWidget().getCaretOffset();
		textViewer.setRedraw(false);
		textViewer.refresh();
		textViewer.getTextWidget().setSelection(off, off);
		textViewer.setRedraw(true);
	}
	
	public void dispose() {
		selectedVariable = null;
		textViewer = null;
	}

	public void clear() {
		// TODO Auto-generated method stub
		
	}

	public IRegion[] match(IDocument doc) throws BadLocationException {
		
		if (doc == null ||  doc.getLength()<1) return null;
		
		final List<IRegion> regions = new ArrayList<IRegion>(7);

		// Any variable substitution
		String text =  doc.get();

		int offset = 0;
		try {
			while(text!=null) {
				final int index = text.indexOf("${");
				if (index<0) break;
	
				final int length  = text.substring(index).indexOf("}")+1;
	            if (length<=0) break;
	            
				final VariableRegion region = new VariableRegion(offset+index, length, VariableType.NORMAL);
				regions.add(region);
				offset = offset+index+length;
				text   = text.substring(index+length);
			}
		} catch (Throwable usuallyIgnored) {
			logger.error("Did not process all variables in file");
		}

		// Selected instances
		if (selectedVariable!=null) {
			
 			final String replace = "${"+selectedVariable.getVariableName()+"}";
			
 			// Assumes small files...
 			text =  doc.get();

 			offset = 0;
			while(text!=null) {
				final int index = text.indexOf(replace);
				if (index<0) break;
			
				final VariableRegion region = new VariableRegion(offset+index, replace.length(), VariableType.SELECTED);
				regions.add(region);
				offset = offset+index+replace.length();
				text   = text.substring(index+replace.length());
			}
			
		}
		
		if (!regions.isEmpty()) return regions.toArray(new IRegion[regions.size()]);

		return null;
	}

}
