package org.dawb.passerelle.actors.data.config;

public interface ISliceInformationProvider {

	/**
	 * Names of datasets
	 * @return
	 */
	String[] getDataSetNames();

	/**
	 * Path of resource
	 * @return
	 */
	String getSourcePath();

}
