package io.konig.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.File;

import io.konig.privacy.deidentification.Application;
import io.konig.privacy.deidentification.repo.DatasourceTrustServiceImpl;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import com.google.common.io.Files;

import io.konig.privacy.deidentification.rest.PersonController;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
 



/**
 * Unit test for simple App.
 */
@RunWith(SpringRunner.class)
@Category(IntegrationTest.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(secure=false)
public class PersonControllerIT 
{
	
	private static Logger logger = LoggerFactory.getLogger(PersonControllerIT.class);
	@Autowired
	private MockMvc mockMvc;
	
	
	@Autowired
	private Environment env;
	
	private static File mavenHome = null;
	
	@Ignore
	public void postSensitivePII() throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String workingdir=System.getProperty("user.dir");
		JsonNode node=mapper.readTree(new File(workingdir+"/src/integration-test/resources/personRequest.json"));
		RequestBuilder requestBuilder = MockMvcRequestBuilders
				.post("/api/privacy/v5/person")
				.accept(MediaType.APPLICATION_JSON).content(node.toString())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();
		
		MockHttpServletResponse response = result.getResponse();
				
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		
		String annotatedPersonExpected = loadAsString(workingdir+"\\src\\integration-test\\resources\\expectedAnnotatedPerson.json");
				
		assertEquals(annotatedPersonExpected,response.getContentAsString());
				
				
		
	}
	
	@Ignore
	public void integrationTestGBI() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String workingdir=System.getProperty("user.dir");
				
		
		JsonNode dataModelNode=mapper.readTree(new File(workingdir+"/src/integration-test/resources/GBIDataModelRequest.json"));
		RequestBuilder requestBuilder = MockMvcRequestBuilders
				.post("/api/schema")
				.accept(MediaType.APPLICATION_JSON).content(dataModelNode.toString())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult dataModelresult = mockMvc.perform(requestBuilder).andReturn();
		
		MockHttpServletResponse dataModelresponse = dataModelresult.getResponse();
		
		assertEquals(HttpStatus.CREATED.value(), dataModelresponse.getStatus());
		
		String version= (String) dataModelresponse.getHeaderValue("Location");
						
		ObjectMapper personMapper = new ObjectMapper();		
		JsonNode personnode=personMapper.readTree(new File(workingdir+"/src/integration-test/resources/GBIPersonRequest.json"));
		RequestBuilder personRequestBuilder = MockMvcRequestBuilders
				.post("/api/privacy/"+version.substring(8)+"/person")
				.accept(MediaType.APPLICATION_JSON).content(personnode.toString())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult personResult = mockMvc.perform(personRequestBuilder).andReturn();
		
		MockHttpServletResponse personResponse = personResult.getResponse();
			
		assertEquals(HttpStatus.CREATED.value(), personResponse.getStatus());
		
		ObjectMapper personResponseMapper=new ObjectMapper();
		
		JsonNode personresponseNode= personResponseMapper.readTree(personResponse.getContentAsString());
		
		String pseudonym=personresponseNode.findValue("pseudonym").textValue();
		
		assertNotNull(pseudonym);
		
		String email=null;
		
		JsonNode personNode = personresponseNode.path("data");
		ArrayNode emailArray = (ArrayNode)personresponseNode.findValue("email");
		for (int j = 0; j < emailArray.size(); j++) {
			TextNode objectValue =  (TextNode) emailArray.get(j);				
			email=objectValue.asText();
		}		
			
		
		assertNotNull(email);
		
		assertEquals("Tanji@example.com",email);
				
	
		RequestBuilder annotatedPersonRequestBuilder = MockMvcRequestBuilders
				.get("/api/privacy/"+version.substring(8)+"/person/"+pseudonym+"/.annotated")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult annotatedPersonResult = mockMvc.perform(annotatedPersonRequestBuilder).andReturn();
		
		MockHttpServletResponse annotatedpersonResponse = annotatedPersonResult.getResponse();
		
		assertEquals(HttpStatus.OK.value(), annotatedpersonResponse.getStatus());
		
		ObjectMapper annotatedPersonMapper = new ObjectMapper();
		
		JsonNode annotationData = annotatedPersonMapper.readTree(annotatedpersonResponse.getContentAsString());
		
		ArrayNode annotationArray= (ArrayNode) annotationData.get("graph");
		boolean emailflag=false;
		for (int i=0; i<annotationArray.size(); i++) {
			JsonNode node = annotationArray.get(i);
			String propertyName = node.get("property").asText();
			String emailValue= node.get("value").asText();
			if ("email".equals(propertyName) && emailValue.equals("Tanji@example.com")) {
				emailflag=true;
				break;
			}
		}
		assertTrue(emailflag);
				
	}
	
	@Test
	public void integrationTest2GBI() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String workingdir=System.getProperty("user.dir");
				
		
		JsonNode dataModelNode=mapper.readTree(new File(workingdir+"/src/integration-test/resources/GBIDataModelRequest.json"));
		RequestBuilder requestBuilder = MockMvcRequestBuilders
				.put("/api/schema/v1")
				.accept(MediaType.APPLICATION_JSON).content(dataModelNode.toString())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult dataModelresult = mockMvc.perform(requestBuilder).andReturn();
		
		MockHttpServletResponse dataModelresponse = dataModelresult.getResponse();
		

		
		assertEquals(HttpStatus.OK.value(), dataModelresponse.getStatus());
		
						
		ObjectMapper personMapper = new ObjectMapper();		
		JsonNode personnode=personMapper.readTree(new File(workingdir+"/src/integration-test/resources/GBIPersonRequest.json"));
		RequestBuilder personRequestBuilder = MockMvcRequestBuilders
				.post("/api/privacy/v1/person")
				.accept(MediaType.APPLICATION_JSON).content(personnode.toString())
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult personResult = mockMvc.perform(personRequestBuilder).andReturn();
		
		MockHttpServletResponse personResponse = personResult.getResponse();
			
		assertEquals(HttpStatus.CREATED.value(), personResponse.getStatus());
		
		ObjectMapper personResponseMapper=new ObjectMapper();
		
		
		logger.info("Response for Post Sensitive PII"+personResponse.getContentAsString());
		
		JsonNode personresponseNode= personResponseMapper.readTree(personResponse.getContentAsString());
		
		String pseudonym=personresponseNode.findValue("pseudonym").textValue();
		
		assertNotNull(pseudonym);
		
		String email=null;
		
		JsonNode personNode = personresponseNode.path("data");
		ArrayNode emailArray = (ArrayNode)personresponseNode.findValue("email");
		for (int j = 0; j < emailArray.size(); j++) {
			TextNode objectValue =  (TextNode) emailArray.get(j);				
			email=objectValue.asText();
		}		
			
		
		assertNotNull(email);
		
		assertEquals("Tanji@example.com",email);
				
	
		RequestBuilder annotatedPersonRequestBuilder = MockMvcRequestBuilders
				.get("/api/privacy/v1/person/"+pseudonym+"/.annotated")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Basic dGVzdDp0ZXN0MTIz");
		
		MvcResult annotatedPersonResult = mockMvc.perform(annotatedPersonRequestBuilder).andReturn();
		
		MockHttpServletResponse annotatedpersonResponse = annotatedPersonResult.getResponse();
		
		assertEquals(HttpStatus.OK.value(), annotatedpersonResponse.getStatus());
		
		ObjectMapper annotatedPersonMapper = new ObjectMapper();
		
		logger.info("Response for Get Annotated PII"+annotatedpersonResponse.getContentAsString());
		
		JsonNode annotationData = annotatedPersonMapper.readTree(annotatedpersonResponse.getContentAsString());
		
		ArrayNode annotationArray= (ArrayNode) annotationData.get("graph");
		boolean emailflag=false;
		for (int i=0; i<annotationArray.size(); i++) {
			JsonNode node = annotationArray.get(i);
			String propertyName = node.get("property").asText();
			String emailValue= node.get("value").asText();
			if ("email".equals(propertyName) && emailValue.equals("Tanji@example.com")) {
				emailflag=true;
				break;
			}
		}
		assertTrue(emailflag);
				
	}
	
	private String loadAsString(String path) throws FileNotFoundException{
		InputStream input = new FileInputStream(path);
		Scanner scanner = new Scanner(input, "UTF-8");
		scanner.useDelimiter("\\A");
		String result = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		return result;		
		
	}
    
}
