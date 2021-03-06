package io.konig.privacy.deidentification.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonWithMetadata;

/**
 * A service that provides access to personally identifiable information and which generates 
 * pseudonyms.
 * 
 * @author Greg McFall
 *
 */
public interface PersonService {
	
	
	
	/**
	 * Post the personal information for a given individual.  
	 * This method merges the supplied description of the Person with any prior information that might have 
	 * been posted earlier.
	 * 
	 * @param metaPerson  A JSON description of the person decorated with metadata.  The JSON description must include
	 * at least one key for the person.
	 * 
	 * @return The pseudonym assigned to the person.  If no pseudonym was previously assigned, one will be generated.
	 */	
	public PersonKeys postSensitivePII(PersonWithMetadata metaPerson) throws HttpClientErrorException, IOException,Exception;
	
	/**
	 * Stream a JSON representation of the Personal Information for a specific individual.
	 * 
	 * This method emits to a writer the same information that is returned by
	 * {@link PersonService#getPersonalInformation(PersonKeys) getPersonalInformation}.
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
	
	/**
	 * To get annotated Sensitive for the specific person
	 * @param version
	 * @param pseudonym
	 * @return
	 * @throws DataAccessException
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	public JsonNode getAnnotatedSensitivePII(String version,String pseudonym) throws DataAccessException, JsonProcessingException, IOException;
	
	public JsonNode getBatchSensitivePII(String version, ArrayList<String> pseudonym) throws DataAccessException,JsonProcessingException, IOException;

}
