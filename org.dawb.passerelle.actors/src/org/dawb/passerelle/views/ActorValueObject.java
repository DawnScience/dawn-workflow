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


/**
 * Bean used in ValueView table model.
 * @author gerring
 *
 */
public class ActorValueObject {

	public enum ActorValueDataType {
		LIST, SCALAR, ROI;
	}
	
	private ActorValueDataType inDataType;
	private ActorValueDataType outDataType;
	private String inputName;
	private String outputName;
	private Object inputValue;
	private Object outputValue;
	
	public String getInputName() {
		return inputName;
	}
	public void setInputName(String inputName) {
		this.inputName = inputName;
	}
	public String getOutputName() {
		return outputName;
	}
	public void setOutputName(String outputName) {
		this.outputName = outputName;
	}
	public Object getInputValue() {
		return inputValue;
	}
	public void setInputValue(Object inputValue) {
		this.inputValue = inputValue;
	}
	public Object getOutputValue() {
		return outputValue;
	}
	public void setOutputValue(Object outputValue) {
		this.outputValue = outputValue;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((inDataType == null) ? 0 : inDataType.hashCode());
		result = prime * result
				+ ((inputName == null) ? 0 : inputName.hashCode());
		result = prime * result
				+ ((inputValue == null) ? 0 : inputValue.hashCode());
		result = prime * result
				+ ((outDataType == null) ? 0 : outDataType.hashCode());
		result = prime * result
				+ ((outputName == null) ? 0 : outputName.hashCode());
		result = prime * result
				+ ((outputValue == null) ? 0 : outputValue.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActorValueObject other = (ActorValueObject) obj;
		if (inDataType != other.inDataType)
			return false;
		if (inputName == null) {
			if (other.inputName != null)
				return false;
		} else if (!inputName.equals(other.inputName))
			return false;
		if (inputValue == null) {
			if (other.inputValue != null)
				return false;
		} else if (!inputValue.equals(other.inputValue))
			return false;
		if (outDataType != other.outDataType)
			return false;
		if (outputName == null) {
			if (other.outputName != null)
				return false;
		} else if (!outputName.equals(other.outputName))
			return false;
		if (outputValue == null) {
			if (other.outputValue != null)
				return false;
		} else if (!outputValue.equals(other.outputValue))
			return false;
		return true;
	}
	public ActorValueDataType getInDataType() {
		return inDataType;
	}
	public void setInDataType(ActorValueDataType dataType) {
		this.inDataType = dataType;
	}
	public ActorValueDataType getOutDataType() {
		return outDataType;
	}
	public void setOutDataType(ActorValueDataType outDataType) {
		this.outDataType = outDataType;
	}
}
