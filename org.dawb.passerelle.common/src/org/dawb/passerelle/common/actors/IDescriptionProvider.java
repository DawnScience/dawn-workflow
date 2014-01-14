package org.dawb.passerelle.common.actors;

import ptolemy.kernel.util.Nameable;

/**
 * Provides a description for an actor or similar entity
 * 
 * @author fcp94556
 *
 */
public interface IDescriptionProvider extends Nameable {

	/**
	 * The description of what the actor does.
	 * @return
	 */
	public String getDescription();
	
	/**
	 * The description of what the actor does.
	 * @param description
	 */
	public void setDescription(String description);
}
