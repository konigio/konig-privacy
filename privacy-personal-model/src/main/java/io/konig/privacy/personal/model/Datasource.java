package io.konig.privacy.personal.model;

import java.util.List;

public class Datasource {

	private String id;
	private String uuid;
	private List<LanguageString> name;
	private double trustLevel;
	
	/**
	 * Get the fully-qualified IRI for this Datasource
	 * @return The fully-qualified IRI for this Datasource
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Set the fully-qualified IRI for this Datasource
	 * @param id The fully-qualified IRI for this Datasource
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Get the list of names for this Datasource in various languages.
	 * @return The list of names for this Datasource in various languages.
	 */
	public List<LanguageString> getName() {
		return name;
	}
	
	/**
	 * Set the list of names for this Datasource in various languages.
	 * @param name The list of names for this Datasource in various languages.
	 */
	public void setName(List<LanguageString> name) {
		this.name = name;
	}
	
	/**
	 * Get the trust level of this Datasource, in the range from [0,1].  Higher
	 * values indicate higher levels of trust.
	 * @return
	 */
	public double getTrustLevel() {
		return trustLevel;
	}
	public void setTrustLevel(double trustLevel) {
		if (trustLevel<0 || trustLevel>1) {
			throw new IllegalArgumentException("The trustLevel must be in the range [0, 1]");
		}
		this.trustLevel = trustLevel;
	}
	
	
	
	
}
