package io.konig.privacy.deidentification.model;

/**
 * An object that encapsulates the identifier for a specific Person within the context of a given Identity Provider.
 * @author Greg McFall
 *
 */
public class Identity {

	private String identityProvider;
	private String identifier;
	
	/**
	 * Create a new Identity
	 * @param identityProvider  The fully-qualified IRI for the identify provider.
	 * @param identifier The identifier for the target Person within the specified identity provider.
	 */
	public Identity(String identityProvider, String identifier) {
		this.identityProvider = identityProvider;
		this.identifier = identifier;
	}

	/**
	 * Get the fully-qualified IRI for the identity provider.
	 * @return The fully-qualified IRI for the identity provider.
	 */
	public String getIdentityProvider() {
		return identityProvider;
	}

	/**
	 * Get the identifier for the target Person.
	 * @return The identifier for the target Person.
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	
	
	
}
