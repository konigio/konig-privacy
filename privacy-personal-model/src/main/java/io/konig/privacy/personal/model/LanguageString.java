package io.konig.privacy.personal.model;

/**
 * An object that encapsulates a String value plus the language in which that value is expressed.
 * @author Greg McFall
 *
 */
public class LanguageString {

	private String language;
	private String value;
	
	/**
	 * Get the BCP-47 language tag for the language in which the string value is expressed.
	 */
	public String getLanguage() {
		return language;
	}
	
	/**
	 * Set the BCP-47 language tag for the language in which the string value is expressed.
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
	
	/**
	 * Get the string value
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Set the string value
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
