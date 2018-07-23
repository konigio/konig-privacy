package io.konig.privacy.deidentification.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * This class to is hold JsonNode of input payload
 *
 */

public class Person {
	private JsonNode person;

	/**
	 * @return the person
	 */
	public JsonNode getPerson() {
		return person;
	}

	/**
	 * @param person
	 *            the person to set
	 */
	public void setPerson(JsonNode person) {
		this.person = person;
	}

}
