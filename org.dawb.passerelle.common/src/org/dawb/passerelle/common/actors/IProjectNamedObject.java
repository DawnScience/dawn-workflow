package org.dawb.passerelle.common.actors;

import org.eclipse.core.resources.IProject;

import ptolemy.actor.Manager;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.NamedObj;

/**
 * Interface designed to be used for Diamond actors such that
 * connection to Passerelle can be reduced (engine can then 
 * be swapped for ptolemy/Kepler if required now that they
 * have an RCP application [not currently planned])
 * 
 * @author Matthew Gerring
 *
 */
public interface IProjectNamedObject {
	
	/**
	 * This is the named Object that usually is
	 * the same object implementing IProjectNamedObject but returned
	 * as a Ptolemy object.
	 * @return
	 */
	public NamedObj getObject();

	/**
	 * The project for the named object. Used to mark
	 * actors as providing the resources project.
	 * 
	 * NOTE: org.eclipse.core.resources does not link an actor to the
	 * eclipse UI as the resources plugin is not UI dependent.
	 * 
	 * @return
	 */
	public IProject getProject();

	/**
	 * The name used in the engine and saved to the moml file.
	 * @return
	 */
	public String getName();

    /**
     * The name displayed to the user.
     * @return
     */
	public String getDisplayName();

	/**
	 * Must have a container.
	 * @return
	 */
	public NamedObj getContainer();

	/**
	 * 
	 * @return
	 */
	public Manager getManager();

	/**
	 * May be called to ask the actor to finish
	 */
	public void requestFinish();

	/**
	 * Get the ptolemy attribute for the actor.
	 * 
	 * @param name
	 * @return
	 */
	public Attribute getAttribute(String name);

	/**
	 * Allows a parameter to be registered (may not work in pure ptolemy) 
	 * @param breakPoint
	 */
	public void registerConfigurableParameter(Parameter breakPoint);

	/**
	 * Allows a parameter to be registered (may not work in pure ptolemy) 
	 * @param breakPoint
	 */
	public void registerExpertParameter(Parameter breakPoint);

}
