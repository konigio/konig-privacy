package io.konig.privacy.deidentification.service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface DataModelService {
	
	public JsonNode getSchemaByVersion(String version) throws DataAccessException, JsonProcessingException, IOException;
	
	public String getLatestDataModel() throws DataAccessException;
	
	public String put(String jsonStr) throws DataAccessException;

}
