package io.konig.privacy.deidentification.service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface DataModelService {
	
	/**
	 * This method is used to retrieve the JSON schema based on the version provided.
	 * @param version
	 * @return
	 * @throws DataAccessException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode getSchemaByVersion(String version) throws DataAccessException, JsonProcessingException, IOException;
	
	/**
	 * This is method is used to get the latest json schema Version.
	 * @return
	 * @throws DataAccessException
	 */
	public String getLatestDataModel() throws DataAccessException;
	
	/**
	 * This method is used to post JSON Schema to service.
	 * @param jsonStr
	 * @return
	 * @throws DataAccessException
	 */
	public String put(String jsonStr) throws DataAccessException;

}
