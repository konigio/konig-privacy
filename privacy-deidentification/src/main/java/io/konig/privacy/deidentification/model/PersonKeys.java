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
	private List<Identity> identity;
	
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
	public List<Identity> getIdentity() {
		return identity;
	}
	
	/**
	 * Set the list of identities for the target person as assigned by various identity providers.
	 * @param identity The list of identities for the target person as assigned by various identity providers.
	 */
	public void setIdentity(List<Identity> identity) {
		this.identity = identity;
	}
	
	

}
