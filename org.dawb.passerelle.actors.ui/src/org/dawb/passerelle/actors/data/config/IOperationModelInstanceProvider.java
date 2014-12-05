package org.dawb.passerelle.actors.data.config;

import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;

public interface IOperationModelInstanceProvider {

	/**
	 * Class to be used for the model
	 * @return
	 */
	Class<? extends IOperationModel> getModelClass() throws Exception;

}
