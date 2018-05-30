package io.konig.privacy.personal.model;

public class AnnotatedProperty extends AnnotatedValue {

	private String name;

	/**
	 * Get the name of the property
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set the name of the property.
	 * @param propertyName
	 */
	public void setName(String propertyName) {
		this.name = propertyName;
	}
	
	
}
