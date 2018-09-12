package io.konig.privacy.deidentification.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.StringTokenizer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.spy.memcached.MemcachedClient;

public class PersonSchemaServiceTest {
	
	private static String VERSION = "1";
	
	@Mock
	MemcachedClient cache;

	@Mock
	DataModelService dataModelService;
	
	private PersonSchemaService schemaService;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		schemaService = new PersonSchemaService(cache, 100, dataModelService);
		
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
		
		when(dataModelService.getSchemaByVersion(VERSION)).thenReturn(dataModelSchema);
		when(cache.get(VERSION)).thenReturn(null);
	}
	
	@Test
	public void test() throws Exception {
		
		String text = schemaService.pseudonymsRequest(VERSION);

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode schemaNode = (ObjectNode) mapper.readTree(text);
		
		assertEquals("object", get(schemaNode, "properties.header.type"));
		assertEquals("string", get(schemaNode, "properties.header.properties.datasource.type"));
		assertEquals("object", get(schemaNode, "properties.data.type"));
		assertEquals("string", get(schemaNode, "properties.data.properties.givenName.type"));
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

}
