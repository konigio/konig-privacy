package io.konig.privacy.deidentification.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.spy.memcached.MemcachedClient;

@Service
public class PersonSchemaService {
	
	private static final String GET_PSEUDONYMS_KEY_PREFIX = "get-pseudonyms-request-v";
	
	@Autowired
	private MemcachedClient cache;
		
	@Autowired
	private DataModelService dataModelService;
	
	@Autowired
	private Environment env;
	
	public PersonSchemaService(MemcachedClient cache, DataModelService dataModelService, Environment env) {
		this.cache = cache;
		this.dataModelService = dataModelService;
		this.env = env;
	}
	
	

	public MemcachedClient getCache() {
		return cache;
	}

	public void setCache(MemcachedClient cache) {
		this.cache = cache;
	}


	public DataModelService getDataModelService() {
		return dataModelService;
	}

	public void setDataModelService(DataModelService dataModelService) {
		this.dataModelService = dataModelService;
	}

	/**
	 * Returns the memcached key under which we store the JSON Schema for the Get Pseudonyms request.
	 * @param dataModelVersion The version of the Data Model that provides the schema for the "data" portion of the request
	 * @return
	 */
	public static String getPseudonymsCacheKey(String dataModelVersion) {
		return GET_PSEUDONYMS_KEY_PREFIX + dataModelVersion;
	}
	
	
	/**
	 * Get the JSON Schema for a request to obtain a batch of pseudonyms
	 * @param dataModelVersion  The version of the Data Model used to represent PII.
	 * @return The text of the JSON Schema
	 * @throws JsonProcessingException
	 * @throws DataAccessException
	 * @throws IOException
	 */
	public String pseudonymsRequest(String dataModelVersion) throws JsonProcessingException, DataAccessException, IOException {

		String key = getPseudonymsCacheKey(dataModelVersion);
		String jsonSchema = (String)cache.get(key);
		if (jsonSchema == null) {
			ObjectNode dataModelJson = (ObjectNode) dataModelService.getSchemaByVersion(dataModelVersion);
			JsonBuilder builder = new JsonBuilder();
			
			
			dataModelJson.remove("$schema");
			JsonNode definitions = dataModelJson.remove("definitions");
			
			ObjectNode jsonSchemaNode = builder.begin()
				.put("$schema", "http://json-schema.org/draft-04/schema#")
				.put("type", "object")
				.beginObject("properties")
					.beginObject("header")
						.put("type", "object")
						.beginObject("properties")
							.beginObject("datasource")
								.put("type", "string")
							.endObject("header")
						.endObject("properties")
					.endObject("header")
					.beginObject("data")
						.put("type", "array")
						.set("items", dataModelJson)
					.endObject("data")
				.endObject("properties")
				.set("definitions", definitions)
				.end();
			
			
			jsonSchema = jsonSchemaNode.toString();
			cache.set(key, Integer.parseInt(env.getProperty("aws.memcache.expirytime")), jsonSchema);
		}
		return jsonSchema;
	}

}
