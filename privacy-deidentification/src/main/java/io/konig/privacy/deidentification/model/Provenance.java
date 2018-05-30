package io.konig.privacy.deidentification.model;

import java.util.GregorianCalendar;

/**
 * Encapsulates information about the provenance of some field.
 * @author Greg McFall
 *
 */
public class Provenance {

	private GregorianCalendar receivedAtTime;
	private String receivedFrom;
	
	/**
	 * Get the time at which this property was received by the server
	 * @return
	 */
	public GregorianCalendar getReceivedAtTime() {
		return receivedAtTime;
	}
	
	/**
	 * Set the time at which this property was received by the server
	 * @param receivedAtTime
	 */
	public void setReceivedAtTime(GregorianCalendar receivedAtTime) {
		this.receivedAtTime = receivedAtTime;
	}
	
	/**
	 * Get fully-qualified IRI of the DataSource that supplied the value of this property.
	 */
	public String getReceivedFrom() {
		return receivedFrom;
	}

	/**
	 * Set fully-qualified IRI of the DataSource that supplied the value of this property.
	 */
	public void setReceivedFrom(String receivedFrom) {
		this.receivedFrom = receivedFrom;
	}
}
