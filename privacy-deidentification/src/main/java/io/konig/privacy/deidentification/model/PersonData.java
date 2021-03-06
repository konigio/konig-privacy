package io.konig.privacy.deidentification.model;

public class PersonData {
	String person;
	String annotated_person;
	String pseudonym;

	/**
	 * Get the Person Data from DB
	 * @return the person
	 */
	public String getPerson() {
		return person;
	}

	/**
	 * Set the the Person Data to be Stored.
	 * @param person
	 *            the person to set
	 */
	public void setPerson(String person) {
		this.person = person;
	}

	public String getAnnotated_person() {
		return annotated_person;
	}

	public void setAnnotated_person(String annotated_person) {
		this.annotated_person = annotated_person;
	}

	/**
	 * Get the Pseudonym for the given Identifier
	 * @return the pseudonym
	 */
	public String getPseudonym() {
		return pseudonym;
	}

	/**
	 * Set the Pseudonym
	 * @param pseudonym
	 *            the pseudonym to set
	 */
	public void setPseudonym(String pseudonym) {
		this.pseudonym = pseudonym;
	}

}
