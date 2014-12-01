package org.dawb.passerelle.actors.data.config;

import java.util.Map;

import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;

/**
 * Used to create a JSON String that can be persisted in the MOML file.
 * @author fcp94556
 *
 */
public class OperationModelBean {
	
	private Map<String, String> specials;
	private IOperationModel     model;
	public OperationModelBean() {
		
	}
	public OperationModelBean(Map<String, String> specials, IOperationModel model) {
		this();
		this.specials = specials;
		this.model = model;
	}
	public Map<String, String> getSpecials() {
		return specials;
	}
	public void setSpecials(Map<String, String> specials) {
		this.specials = specials;
	}
	public IOperationModel getModel() {
		return model;
	}
	public void setModel(IOperationModel model) {
		this.model = model;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result
				+ ((specials == null) ? 0 : specials.hashCode());
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
		OperationModelBean other = (OperationModelBean) obj;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (specials == null) {
			if (other.specials != null)
				return false;
		} else if (!specials.equals(other.specials))
			return false;
		return true;
	}

}
