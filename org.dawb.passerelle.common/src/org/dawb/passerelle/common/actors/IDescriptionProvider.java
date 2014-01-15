package org.dawb.passerelle.common.actors;

import ptolemy.kernel.util.Nameable;

/**
 * Provides a description for an actor or similar entity
 * 
 * @author fcp94556
 *
 */
public interface IDescriptionProvider extends Nameable {
	
	public enum Requirement {
		ESSENTIAL, OPTIONAL, EXPERT, DEPRECATED;
	}
	
	public enum VariableHandling {
		NONE("Standard attribute"), EXPAND("Expanded attribute\ne.g. ${file_name} ..."), EXPRESSION("Expression attribute\ne.g. 2*sin(x) ...");
		
		private String label;

		VariableHandling(String label) {
			this.label = label;
		}
		
		public String toString() {
			return label;
		}
	}

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
	
	/**
	 * 
	 * @param key TODO
	 * @param description
	 * @return
	 */
	public String getDescription(Object key);
	/**
	 * 
	 * @param key TODO
	 * @param description
	 * @return
	 */
	public Requirement getRequirement(Object key);
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public VariableHandling getVariableHandling(Object key);
	
	/**
	 * 
	 * @param key
	 * @param description
	 * @return
	 */
	public void setDescription(Object key, Requirement requirement, VariableHandling variable, String description);

}
