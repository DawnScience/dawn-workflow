/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 

package org.dawb.passerelle.common.parameter.function;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.castor.core.util.Base64Decoder;
import org.castor.core.util.Base64Encoder;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.Fermi;

import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

/**
 * This parameter can be used to edit a Mathematical Function in the workflow.
 * 
 * It pops up a dialog for the Function values.
 * 
 *
 */
public class FunctionParameter extends StringParameter  implements CellEditorAttribute{
	
	private static final long serialVersionUID = -8931922295061360172L;
	private static final Logger logger = LoggerFactory.getLogger(FunctionParameter.class);

	public FunctionParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}


	@Override
	public CellEditor createCellEditor(Control control) {
		
		final DialogCellEditor editor = new DialogCellEditor((Composite)control) {
			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
								
				final FunctionDialog dialog = new FunctionDialog(cellEditorWindow.getShell(), getContainer()); // extends BeanDialog
				dialog.create();
				dialog.getShell().setSize(650,500); // Windows needs slightly larger size.
				dialog.getShell().setText("Edit Function");
			
				try {
					dialog.setFunction(getFunctionFromValue());
			        final int ok = dialog.open();
			        if (ok == Dialog.OK) {
			            return getValueFromFunction(dialog.getFunction());
			        }
				} catch (Exception ne) {
					logger.error("Problem decoding and/or encoding bean!", ne);
				}
		        
		        return null;
			}
		    protected void updateContents(Object value) {
		        if ( getDefaultLabel() == null) {
					return;
				}
		        getDefaultLabel().setText(getRendererText());
		    }

		};
		
		
		return editor;
	}

	@Override
	public String getRendererText() {
		if (getExpression()==null || "".equals(getExpression())) return "Click to define function...";
		try {
			final AFunction function = getFunctionFromValue();
			return function.getClass().getSimpleName()+"  "+function.toString();
		} catch (Throwable e) {
			return "Click to define function...";
		} 
	}

	/**
	 * Decode the function from a string.
	 * @return
	 */
	private AFunction getFunctionFromValue() throws IOException, ClassNotFoundException{
		
		if (getExpression()==null || "".equals(getExpression())) {
			Fermi fermi = new Fermi(new double[]{0,0,0,0});
			return fermi;
		}

		return getFunctionFromValue(getExpression());
		
	}
	
	private AFunction getFunctionFromValue(String expression) throws IOException, ClassNotFoundException {
		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		ObjectInputStream ois=null;
		try {
			Thread.currentThread().setContextClassLoader(AFunction.class.getClassLoader());
			byte[] data = Base64Decoder.decode(getExpression());
			ois = new ObjectInputStream(new ByteArrayInputStream(data));
			Object o  = ois.readObject();
			return (AFunction)o;
		} finally {
			Thread.currentThread().setContextClassLoader(original);
			if (ois!=null) ois.close();
		}
	}


	public AFunction getFunction() {
		if (getExpression()==null || "".equals(getExpression())) return null;
		try {
			return getFunctionFromValue(getExpression());
		} catch (Exception ne) {
			return null;
		}
	}

	/**
	 * Encode the function as a string, base64 encode probably
	 * @param function
	 * @return
	 */
	private String getValueFromFunction(final AFunction function) throws IOException {
		if (function==null) return "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject( function );
		oos.close();
		return new String(Base64Encoder.encode(baos.toByteArray()));
	}

	public void setFunction(AFunction function) {
		try {
			setExpression(getValueFromFunction(function));
		} catch (IOException e) {
			setExpression("");
		}
	}

}