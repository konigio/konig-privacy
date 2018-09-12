package io.konig.privacy.deidentification.model;

import java.util.Objects;

/**
 * An object that encapsulates the identifier for a specific Person within the context of a given Identity Provider.
 * @author Greg McFall
 *
 */
public class IdentifiedBy {

	private String identityProvider;
	private String identifier;
	
	/**
	 * Create a new Identity
	 * @param identityProvider  The fully-qualified IRI for the identify provider.
	 * @param identifier The identifier for the target Person within the specified identity provider.
	 */
	public IdentifiedBy(String identityProvider, String identifier) {
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
	
	@Override
    public boolean equals(Object o) {

		if (o == this)
			return true;
		if (!(o instanceof IdentifiedBy)) {
			return false;
		}
		IdentifiedBy identity = (IdentifiedBy) o;
		return identity.getIdentityProvider().equals(identityProvider) && identity.getIdentifier().equals(identifier);
	}
	
	@Override
    public int hashCode() {
        return Objects.hash(identityProvider, identifier);
    }
	
	
}
