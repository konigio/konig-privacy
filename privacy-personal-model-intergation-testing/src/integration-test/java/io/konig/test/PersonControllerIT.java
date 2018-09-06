package io.konig.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
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
import java.io.File;

import io.konig.privacy.deidentification.Application;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;

import java.util.Collections;
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
				.contentType(MediaType.APPLICATION_JSON);
		
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();
		
		MockHttpServletResponse response = result.getResponse();
				
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		
		String annotatedPersonExpected = loadAsString(workingdir+"\\src\\integration-test\\resources\\expectedAnnotatedPerson.json");
				
		assertEquals(annotatedPersonExpected,response.getContentAsString());
				
				
		
	}
	
	@Test
	public void integrationTestGBI() throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		String workingdir=System.getProperty("user.dir");
		JsonNode dataModelNode=mapper.readTree(new File(workingdir+"/src/integration-test/resources/GBIDataModelRequest.json"));
		RequestBuilder requestBuilder = MockMvcRequestBuilders
				.post("/api/schema")
				.accept(MediaType.APPLICATION_JSON).content(dataModelNode.toString())
				.contentType(MediaType.APPLICATION_JSON);
		
		MvcResult dataModelresult = mockMvc.perform(requestBuilder).andReturn();
		
		MockHttpServletResponse dataModelresponse = dataModelresult.getResponse();
		
		assertEquals(HttpStatus.CREATED.value(), dataModelresponse.getStatus());
		
		String version= (String) dataModelresponse.getHeaderValue("Location");
						
		ObjectMapper personMapper = new ObjectMapper();		
		JsonNode personnode=personMapper.readTree(new File(workingdir+"/src/integration-test/resources/GBIPersonRequest.json"));
		RequestBuilder personRequestBuilder = MockMvcRequestBuilders
				.post("/api/privacy/"+version.substring(8)+"/person")
				.accept(MediaType.APPLICATION_JSON).content(personnode.toString())
				.contentType(MediaType.APPLICATION_JSON);
		
		MvcResult personResult = mockMvc.perform(personRequestBuilder).andReturn();
		
		MockHttpServletResponse personResponse = personResult.getResponse();
			
		assertEquals(HttpStatus.CREATED.value(), personResponse.getStatus());
		
		ObjectMapper personResponseMapper=new ObjectMapper();
		
		JsonNode personresponseNode= personResponseMapper.readTree(personResponse.getContentAsString());
		
		String pseudonym=personresponseNode.findValue("pseudonym").textValue();
		
		RequestBuilder annotatedPersonRequestBuilder = MockMvcRequestBuilders
				.get("/api/privacy/"+version.substring(8)+"/person/"+pseudonym+"/.annotated")
				.contentType(MediaType.APPLICATION_JSON);
		
		MvcResult annotatedPersonResult = mockMvc.perform(annotatedPersonRequestBuilder).andReturn();
		
		MockHttpServletResponse annotatedpersonResponse = annotatedPersonResult.getResponse();
		
		assertEquals(HttpStatus.OK.value(), annotatedpersonResponse.getStatus());
		
		
	}
	
	private String loadAsString(String path) throws FileNotFoundException{
		System.out.println("Path"+path);
		InputStream input = new FileInputStream(path);
		System.out.println("input"+input);
		/*if (input == null) {
			throw new RuntimeException("Resource not found: " + path);
		}*/
		Scanner scanner = new Scanner(input, "UTF-8");
		scanner.useDelimiter("\\A");
		String result = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		return result;		
		
	}
    
}
