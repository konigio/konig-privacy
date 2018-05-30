package io.konig.privacy.personal.model;

import java.util.GregorianCalendar;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A composite object that encapsulates a JSON description of some personal information,
 * decorated with metadata that applies to all fields within the JSON.
 * 
 * @author Greg McFall
 *
 */
public class DecoratedPersonalInformation {

	private Datasource datasource;
	private GregorianCalendar receivedAtTime;
	private JsonNode json;
	
	/**
	 * Get information about the datasource that provided the JSON description of the personal information
	 */
	public Datasource getDatasource() {
		return datasource;
	}
	
	/**
	 * Set information about the datasource that provided the JSON description of the personal information
	 */
	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}
	
	/**
	 * Get the date/time when the JSON was received.
	 */
	public GregorianCalendar getReceivedAtTime() {
		return receivedAtTime;
	}
	
	/**
	 * Set the date/time when the JSON was received
	 */
	public void setReceivedAtTime(GregorianCalendar receivedAtTime) {
		this.receivedAtTime = receivedAtTime;
	}
	
	/**
	 * Get the JSON representation of the personal information received from the specified datasource.
	 */
	public JsonNode getJson() {
		return json;
	}
	
	/**
	 * Set the JSON representation of the personal information received from the specified datasource.
	 */
	public void setJson(JsonNode json) {
		this.json = json;
	}
	
	
	
	
}
