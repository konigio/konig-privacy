package io.konig.privacy.deidentification.model;

import java.util.List;

/**
 * An object that encapsulates the unique keys used to identify an individual.
 * @author Greg McFall
 *
 */
public class PersonKeys {
	
	private String pseudonym;
	private List<String> email;
	private List<IdentifiedBy> identifiedBy;
	private String id;
	/**
	 * Get the pseudonym for the target person as defined by the privacy services
	 * @return The pseudonym for the target person as defined by the privacy services
	 */
	public String getPseudonym() {
		return pseudonym;
	}
	
	/**
	 * Set the pseudonym for the target person as defined by the privacy services
	 * @param pseudonym The pseudonym for the target person as defined by the privacy services
	 */
	public void setPseudonym(String pseudonym) {
		this.pseudonym = pseudonym;
	}
	
	/**
	 * Get the email addresses for the target person.
	 * 
	 * @return The email addresses for the target person.
	 */
	public List<String> getEmail() {
		return email;
	}
	
	/**
	 * Set the email addresses for the target person.
	 * @param email The email addresses for the target person.
	 */
	public void setEmail(List<String> email) {
		this.email = email;
	}
	
	/**
	 * Get the list of identities for the target person as assigned by various identity providers.
	 * @return The list of identities for the target person as assigned by various identity providers.
	 */
	public List<IdentifiedBy> getIdentifiedBy() {
		return identifiedBy;
	}
	
	/**
	 * Set the list of identities for the target person as assigned by various identity providers.
	 * @param identity The list of identities for the target person as assigned by various identity providers.
	 */
	public void setIdentifiedBy(List<IdentifiedBy> identifiedBy) {
		this.identifiedBy = identifiedBy;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	

}
