package io.konig.privacy.personal.model;

public class AnnotatedValue extends Provenance implements AnnotatedEntity {

	private Object value;

	
	/**
	 * Get the value of this property.
	 * @return An object of one of the following types: Long, Double, String, LanguageString, List<?>, AnnotatedObject
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Set the value of this property.
	 * @param value An object that expresses the value of this property.
	 * @throws IllegalArgumentException if the value is not one of the following types: 
	 * Long, Double, String, LanguageString, AnnotatedEntityList, AnnotatedObject
	 */
	public void setValue(Object value) throws IllegalArgumentException {
		
		if (
			!(value instanceof Long) &&
			!(value instanceof Double) &&
			!(value instanceof LanguageString) &&
			!(value instanceof AnnotatedEntityList) &&
			!(value instanceof AnnotatedObject)
		) {
			throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName());
		}
		this.value = value;
	}
}
