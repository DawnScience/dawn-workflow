package org.dawnsci.workflow.ui.updater;

import java.util.Map;

import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

/**
 * Interface used to update programmatically a moml file
 * @author wqk87977
 *
 */
public interface IWorkflowUpdater {

	/**
	 * Initialize a model file
	 */
	public void initialize();

	/**
	 * Method that updates a region actor
	 * @param actorName the unique actor's name
	 * @param roi the region of interest
	 */
	public void updateRegionActor(String actorName, IROI roi);

	/**
	 * Method that updates a scalar actor
	 * @param actorName the unique actor's name
	 * @param scalar value parameter
	 */
	public void updateScalarActor(String actorName, double scalarValue);

	/**
	 * Method that updates a function actor
	 * @param actorName the unique actor's name
	 * @param function parameter
	 */
	public void updateFunctionActor(String actorName, AFunction function);

	/**
	 * Method that reads a scalar actor's parameter
	 * @param actorName the unique actor's name
	 * @param scalar name parameter
	 * @return the parameter value
	 */
	public String getActorParam(String actorName, String paramName);

	/**
	 * Method that reads a function actor
	 * @param actorName the unique actor's name
	 * @return function
	 */
	public AFunction getFunctionFromActor(String actorName);

	/**
	 * Method that updates an actor's parameter with a given value
	 * @param actorName the unique actor's name
	 * @param paramName the parameter name
	 * @param paramValue the parameter value
	 */
	public void updateActorParam(String actorName, String paramName, String paramValue);

	/**
	 * Method that sets the gold file path
	 * @param path
	 */
	public void setGoldFilePath(String path);

	/**
	 * Method that updates an input actor
	 * @param actorName 
	 *              the unique actor's name
	 * @param dataFilePaths
	 *              key value pair of full file paths and their name
	 */
	public void updateInputActor(String actorName, Map<String, String> dataFilePaths);
}
