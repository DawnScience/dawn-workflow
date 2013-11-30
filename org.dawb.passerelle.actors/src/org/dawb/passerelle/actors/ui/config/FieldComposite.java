/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.ui.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.ui.util.GridUtils;
import org.dawnsci.common.richbeans.beans.IFieldWidget;
import org.dawnsci.common.richbeans.components.file.FileBox;
import org.dawnsci.common.richbeans.components.scalebox.NumberBox;
import org.dawnsci.common.richbeans.components.scalebox.StandardBox;
import org.dawnsci.common.richbeans.components.selector.VerticalListEditor;
import org.dawnsci.common.richbeans.components.wrappers.BooleanWrapper;
import org.dawnsci.common.richbeans.components.wrappers.ComboWrapper;
import org.dawnsci.common.richbeans.components.wrappers.SpinnerWrapper;
import org.dawnsci.common.richbeans.components.wrappers.TextWrapper;
import org.dawnsci.common.richbeans.event.ValueAdapter;
import org.dawnsci.common.richbeans.event.ValueEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * A dialog which can be used with the Rich Beans Framework
 * @author gerring
 *
 */
public class FieldComposite extends Composite {
	
	private List<Control> propControls;
	private CLabel        textLimitLabel, upperBoundLabel, lowerBoundLabel, unitLabel, folderLabel, /**fileFilterLabelLabel,**/ extensionsLabel, choicesLabel;
	private Group         properties;

	// Field Widgets
	private TextWrapper    variableName;
	private ComboWrapper   uiClass;
	private TextWrapper    uiLabel;
	private TextWrapper    defaultValue;
	//private TextWrapper    defaultValueText;
	//private NumberBox      defaultValueNumber;
	//private TextWrapper    defaultValueFile;
	private TextWrapper    unit;
	private SpinnerWrapper textLimit;
	private NumberBox      upperBound, lowerBound;
	private BooleanWrapper folder;
	private BooleanWrapper password;
	//private TextWrapper    fileFilterLabel;
	private TextWrapper    extensions;
	private VerticalListEditor textChoices;
	

	public FieldComposite(Composite parent, int style) {
		super(parent, style);
		create();
	}

	private void create() {
		
		setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(this);
		
		final Composite top = new Composite(this, SWT.NONE);
		top.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		top.setLayout(new GridLayout(2, false));
	    
		CLabel label = new CLabel(top, SWT.NONE);
	    label.setText("Scalar Variable");
		
		this.variableName = new TextWrapper(top, SWT.NONE);
		variableName.setTextType(TextWrapper.TEXT_TYPE.EXPRESSION);
		variableName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		variableName.setToolTipText("The variable name to use in the scalar value entered by the user.");
	
		CLabel labelLabel = new CLabel(top, SWT.NONE);
		labelLabel.setText("Label");

		this.uiLabel = new TextWrapper(top, SWT.NONE);
		uiLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		uiLabel.setToolTipText("If left blank, the variable name will be used for the label.");

		label = new CLabel(top, SWT.NONE);
	    label.setText("Input Type");
	    
		uiClass = new ComboWrapper(top, SWT.READ_ONLY);
		uiClass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final Map<String,String> items = new LinkedHashMap<String,String>(3);
		items.put("Text",    TextWrapper.class.getName());
		items.put("Real",    StandardBox.class.getName());
		items.put("Integer", SpinnerWrapper.class.getName());
		items.put("File",    FileBox.class.getName());
		items.put("Text Choice",ComboWrapper.class.getName());
		uiClass.setItems(items);
		uiClass.setToolTipText("Please set data input type for the user.");
		uiClass.addValueListener(new ValueAdapter() {			
			@Override
			public void valueChangePerformed(ValueEvent e) {
				if (uiClass.getValue()==null) return;
				updateVisibleProperties(uiClass.getValue());
			}
		});
		
		CLabel defValueLabel = new CLabel(top, SWT.NONE);
		defValueLabel.setText("Default Value");
		
		this.defaultValue = new TextWrapper(top, SWT.NONE);
		defaultValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		properties = new Group(this, SWT.NONE);
		properties.setLayout(new GridLayout(2, false));
		properties.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		properties.setText("Text Input Properties");
		propControls = new ArrayList<Control>(7);
				
		folderLabel = new CLabel(properties, SWT.NONE);
		propControls.add(folderLabel);
		folderLabel.setText("Choose Folders");
		
		this.folder = new BooleanWrapper(properties, SWT.NONE);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		propControls.add(folder);
		folder.addValueListener(new ValueAdapter() {
			@Override
			public void valueChangePerformed(ValueEvent e) {
				updateFileVis();
			}
		});
		
//		CLabel defValueLabel = new CLabel(properties, SWT.NONE);
//		defValueLabel.setText("Default Value");
//		
//		this.defaultValueText = new TextWrapper(properties, SWT.NONE);
//		defaultValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		propControls.add(defaultValueText);
//		
//		this.defaultValueNumber = new StandardBox(properties, SWT.NONE);
//		defaultValueNumber.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		propControls.add(defaultValueNumber);
//		
//		this.defaultValueFile = new TextWrapper(properties, SWT.NONE);
//		defaultValueFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		propControls.add(defaultValueFile);

		// Properties conditional to type:
		textLimitLabel = new CLabel(properties, SWT.NONE);
		propControls.add(textLimitLabel);
		textLimitLabel.setText("Text Limit");
		
		this.textLimit    = new SpinnerWrapper(properties, SWT.NONE);
		propControls.add(textLimit);
		textLimit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textLimit.setMinimum(0);
		textLimit.setMaximum(512);
		textLimit.setIncrement(1);
		textLimit.setValue(0);
		textLimit.setToolTipText("A value of 0 means no text limit.");
		
		this.password = new BooleanWrapper(properties, SWT.CHECK);
		password.setText("   Password");		
		password.setToolTipText("Turn on to make the text a password text box.");
		password.setLayoutData(new GridData(0, 0, false, false, 2, 1));
		password.setValue(Boolean.FALSE);
		propControls.add(password);
		
		unitLabel = new CLabel(properties, SWT.NONE);
		propControls.add(unitLabel);
		unitLabel.setText("Unit");
		
		this.unit    = new TextWrapper(properties, SWT.NONE);
		propControls.add(unit);
		unit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lowerBoundLabel = new CLabel(properties, SWT.NONE);
		propControls.add(lowerBoundLabel);
		lowerBoundLabel.setText("Lower Limit");
		
		this.lowerBound    = new StandardBox(properties, SWT.NONE);
		//defaultValueNumber.setMinimum(lowerBound);
		propControls.add(lowerBound);
		lowerBound.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lowerBound.setMinimum(-10000);
		lowerBound.setMaximum(10000);
		lowerBound.setValue(0);
		lowerBound.setDecimalPlaces(6);

		upperBoundLabel = new CLabel(properties, SWT.NONE);
		propControls.add(upperBoundLabel);
		upperBoundLabel.setText("Upper Limit");
		
		this.upperBound    = new StandardBox(properties, SWT.NONE);
		//defaultValueNumber.setMaximum(upperBound);
		propControls.add(upperBound);
		upperBound.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		upperBound.setMinimum(lowerBound);
		upperBound.setMaximum(10000);
		upperBound.setValue(1000);
		upperBound.setDecimalPlaces(6);
		
//		fileFilterLabelLabel = new CLabel(properties, SWT.NONE);
//		propControls.add(fileFilterLabelLabel);
//		fileFilterLabelLabel.setText("File Filter Label");
//
//		fileFilterLabel = new TextWrapper(properties, SWT.NONE);
//		propControls.add(fileFilterLabel);
//		fileFilterLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		extensionsLabel = new CLabel(properties, SWT.NONE);
		propControls.add(extensionsLabel);
		extensionsLabel.setText("Extensions");

		extensions = new TextWrapper(properties, SWT.NONE);
		propControls.add(extensions);
		extensions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		extensions.setToolTipText("Comma separated file extension list");
		
		this.choicesLabel = new CLabel(properties, SWT.NONE);
		propControls.add(choicesLabel);
		choicesLabel.setText("Choices");
		
		this.textChoices  = new VerticalListEditor(properties, SWT.NONE);
		textChoices.setRequireSelectionPack(false);
		textChoices.setListenerName("Text Choices Listener");
		propControls.add(textChoices);
		textChoices.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textChoices.setMinItems(0);
		textChoices.setMaxItems(25);
		textChoices.setDefaultName("Text Choice");
		textChoices.setEditorClass(StringValueBean.class);
		textChoices.setEditorUI(new TextValueComposite(textChoices, SWT.NONE));
		textChoices.setNameField("textValue");
		GridUtils.setVisible(textChoices, false);

		updateVisibleProperties();
	}
	
	protected void updateVisibleProperties() {
		updateVisibleProperties(uiClass.getValue());
	}
	
	private void updateFileVis() {
		boolean isFile = !folder.getValue();
		extensionsLabel.setEnabled(isFile);
		extensions.setEnabled(isFile);
	}

	private void updateVisibleProperties(Object uiValue) {
		
		if (properties==null) return;
		for (Control widget : propControls) GridUtils.setVisible(widget, false);
	    
		if (uiValue==null) uiValue = TextWrapper.class.getName();
		
		if (uiValue.equals(TextWrapper.class.getName())) {
			properties.setText("Text Input Properties");
			//GridUtils.setVisible(defaultValueText, true);
			GridUtils.setVisible(textLimitLabel, true);
			GridUtils.setVisible(textLimit,      true);
			GridUtils.setVisible(password,     true);
			
		} else if (uiValue.equals(ComboWrapper.class.getName())) {
			properties.setText("Choice Input Properties");
			//GridUtils.setVisible(defaultValueText, true);
			GridUtils.setVisible(choicesLabel, true);
			GridUtils.setVisible(textChoices, true);
		
		} else if (uiValue.equals(StandardBox.class.getName())) {
			properties.setText("Real Input Properties");
			lowerBound.setIntegerBox(false);
			upperBound.setIntegerBox(false);
			//defaultValueNumber.setIntegerBox(false);
			//GridUtils.setVisible(defaultValueNumber, true);
			GridUtils.setVisible(lowerBoundLabel, true);
			GridUtils.setVisible(lowerBound,      true);
			GridUtils.setVisible(upperBoundLabel, true);
			GridUtils.setVisible(upperBound,      true);
			GridUtils.setVisible(unitLabel,       true);
			GridUtils.setVisible(unit,            true);
			
		} else if (uiValue.equals(SpinnerWrapper.class.getName())) {
			properties.setText("Integer Input Properties");
			lowerBound.setIntegerBox(true);
			upperBound.setIntegerBox(true);
			//defaultValueNumber.setIntegerBox(true);
			//GridUtils.setVisible(defaultValueNumber, true);
			GridUtils.setVisible(lowerBoundLabel, true);
			GridUtils.setVisible(lowerBound,      true);
			GridUtils.setVisible(upperBoundLabel, true);
			GridUtils.setVisible(upperBound,      true);
			//GridUtils.setVisible(unitLabel,       true);
			//GridUtils.setVisible(unit,            true);
			
		} else if (uiValue.equals(FileBox.class.getName())) {
			properties.setText("File Input Properties");
			//GridUtils.setVisible(defaultValueFile, true);
			GridUtils.setVisible(folderLabel,      true);
			GridUtils.setVisible(folder,           true);
//			GridUtils.setVisible(fileFilterLabelLabel, true);
//			GridUtils.setVisible(fileFilterLabel,      true);
			GridUtils.setVisible(extensionsLabel,      true);
			GridUtils.setVisible(extensions,           true);
			updateFileVis();
		}
		
		properties.layout(propControls.toArray(new Control[propControls.size()]));
		layout();
		getParent().layout();
		getShell().layout();
	}

	// BeanUI methods for synchronizing.
	public IFieldWidget getDefaultValue() {
	
		return defaultValue;
//	    Object uiValue = uiClass.getValue();
//	    if (uiValue==null) uiValue = TextWrapper.class.getName();
//		if (uiValue.equals(TextWrapper.class.getName()) || 
//		    uiValue.equals(ComboWrapper.class.getName())) {
//			return defaultValueText;
//			
//		} else if (uiValue.equals(StandardBox.class.getName())) {
//			
//			return defaultValueNumber;
//			
//		} else if (uiValue.equals(SpinnerWrapper.class.getName())) {
//			return defaultValueNumber;
//			
//		} else if (uiValue.equals(FileBox.class.getName())) {
//		    return defaultValueFile;
//		}
//		return null;
	}
	
	public ComboWrapper getUiClass() {
		return uiClass;
	}

	public SpinnerWrapper getTextLimit() {
		return textLimit;
	}

	public TextWrapper getUnit() {
		return unit;
	}

	public NumberBox getUpperBound() {
		return upperBound;
	}

	public NumberBox getLowerBound() {
		return lowerBound;
	}

	public BooleanWrapper getFolder() {
		return folder;
	}

	public TextWrapper getUiLabel() {
		return uiLabel;
	}

//	public TextWrapper getFileFilterLabel() {
//		return fileFilterLabel;
//	}

	public TextWrapper getExtensions() {
		return extensions;
	}

	public VerticalListEditor getTextChoices() {
		return textChoices;
	}
	public TextWrapper getVariableName() {
		return variableName;
	}

	public BooleanWrapper getPassword() {
		return password;
	}
}
