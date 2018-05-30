package io.konig.privacy.deidentification.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A description of some data model.
 * @author Greg McFall
 *
 */
public class DataModel {

	private String id;
	private String version;
	private JsonNode jsonSchema;
	
	/**
	 * Get the fully-qualified URI for the data model.
	 * @return The fully-qualified URI for the data model.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Set the fully-qualified URI for the data model.
	 * @param id The fully-qualified URI for the data model.
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Get the version number of the data model.
	 * @return The version number of the data model.
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Set the version number of the data model.
	 * @param version The version number of the data model.
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	
	/**
	 * Get the JSON Schema description of the data model.
	 * @return The JSON Schema description of the data model.
	 */
	public JsonNode getJsonSchema() {
		return jsonSchema;
	}
	
	/**
	 * Set the JSON Schema description of the data model.
	 * @param jsonSchema The JSON Schema description of the data model.
	 */
	public void setJsonSchema(JsonNode jsonSchema) {
		this.jsonSchema = jsonSchema;
	}
	
	
}
