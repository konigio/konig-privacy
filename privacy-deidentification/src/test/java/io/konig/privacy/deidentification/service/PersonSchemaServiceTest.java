package io.konig.privacy.deidentification.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;

import io.konig.privacy.deidentification.utils.ValidationUtils;
import net.spy.memcached.MemcachedClient;

public class PersonSchemaServiceTest {
	
	private static String VERSION = "1";
	
	private static String VERSION_2 = "2";
	
	@Mock
	MemcachedClient cache;
	
	@Mock
	Environment env;

	@Mock
	DataModelService dataModelService;
	
	private PersonSchemaService schemaService;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		schemaService = new PersonSchemaService(cache, dataModelService, env);
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		
		JsonBuilder builder = new JsonBuilder();
		
		ObjectNode dataModelSchema = builder
			.begin()
				.put("type", "object")
				.beginObject("properties")
					.beginObject("givenName")
						.put("type", "string")
					.endObject("givenName")
				.endObject("properties")
			.end();
		
		ObjectNode dataModelJsonSchema = loadJson("PersonRepositoryTest/GBIDataModelSchema.json");
		
		when(dataModelService.getSchemaByVersion(VERSION)).thenReturn(dataModelSchema);
		
		when(dataModelService.getSchemaByVersion(VERSION_2)).thenReturn(dataModelJsonSchema);
		
		when(cache.get(VERSION)).thenReturn(null);
		
		when(cache.set(VERSION, Integer.parseInt("1"), dataModelJsonSchema)).thenReturn(null);
		when(env.getProperty("aws.memcache.expirytime")).thenReturn("500");
	}
	
	@Test
	public void test() throws Exception {
		
		String text = schemaService.pseudonymsRequest(VERSION);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode schemaNode = (ObjectNode) mapper.readTree(text);
		
		assertEquals("object", get(schemaNode, "properties.header.type"));
		assertEquals("string", get(schemaNode, "properties.header.properties.datasource.type"));
		assertEquals("array", get(schemaNode, "properties.data.type"));
		assertEquals("string", get(schemaNode, "properties.data.items.properties.givenName.type"));
	}

	private String get(JsonNode node, String path) throws Exception {
		StringTokenizer tokens = new StringTokenizer(path, ".");
		while (tokens.hasMoreTokens()) {
			String fieldName = tokens.nextToken();
			node = node.get(fieldName);
			if (node == null) {
				throw new Exception("Field " + fieldName + "not found in path " + path);
			}
		}
		return node.asText();
	}
	
	private ObjectNode loadJson(String path) throws Exception {
		InputStream input = getClass().getClassLoader().getResourceAsStream(path);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree(input);
		input.close();

		return node;
	}
	
	@Test
	public void jsonValidationTest() throws Exception {
		
		String dataModelSchema = schemaService.pseudonymsRequest(VERSION_2);
		
		ObjectNode personrequest = loadJson("PersonRepositoryTest/GBIPersonRequest.json");
		

        final JsonSchema schemaNode = ValidationUtils.getSchemaNode(dataModelSchema);
        ProcessingReport report = schemaNode.validate(personrequest);
        
        Iterator<ProcessingMessage> sequence = report.iterator();
        while (sequence.hasNext()) {
        	ProcessingMessage msg = sequence.next();
        	System.out.println(msg.getMessage());
        }
        
		
		boolean validation=ValidationUtils.isJsonValid(dataModelSchema, personrequest.toString());
		
		assertTrue(validation);

	}

}
