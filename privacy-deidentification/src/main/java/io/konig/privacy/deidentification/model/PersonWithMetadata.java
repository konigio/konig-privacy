package io.konig.privacy.deidentification.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A composite object that encapsulates a JSON description of some Person,
 * decorated with provenance data that applies to all fields within the JSON.
 * 
 * @author Greg McFall
 *
 */
public class PersonWithMetadata {

	private Metadata metadata;
	private JsonNode person;
	
	public Metadata getMetadata() {
		return metadata;
	}
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	public JsonNode getPerson() {
		return person;
	}
	public void setPerson(JsonNode person) {
		this.person = person;
	}
}
