package io.konig.privacy.personal.model;

import java.io.IOException;
import java.io.PrintWriter;

import com.fasterxml.jackson.databind.JsonNode;

public interface PersonalInformationService {
	
	
	
	/**
	 * Post the personal information for a given individual.  
	 * If a record for the individual exists, this 
	 * @param personalInformation
	 * @return
	 */
	String post(JsonNode personalInformation);
	
	/**
	 * Stream a JSON representation of the Personal Information for a specific individual.
	 * 
	 * This method emits to a writer the same information that is returned by
	 * {@link PersonalInformationService#getPersonalInformation(PersonKeys) getPersonalInformation}.
	 * 
	 * @param keys  The set of known keys for the individual.
	 * @param writer A writer into which the Personal Information in JSON format will be emitted.
	 * @return True if a JSON representation was emitted and false otherwise.
	 * @throws IOException
	 */
	boolean streamPersonalInformation(PersonKeys keys, PrintWriter writer) throws IOException;
	
	/**
	 * Stream the Personal Information for a specific individual in JSON format with provenance annotations.
	 * @param keys  The set of known keys for the individual.
	 * @param writer The writer into which the JSON document will be emitted.
	 * @return True if a JSON document was emitted and false otherwise.
	 * @throws IOException
	 */
	boolean streamAnnotatedPersonalInformation(PersonKeys keys, PrintWriter writer) throws IOException;

}
