package io.konig.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

//import io.konig.privacy.deidentification.Application;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;

import java.util.Collections;

import com.google.common.io.Files;

 



/**
 * Unit test for simple App.
 */
@RunWith(SpringRunner.class)
@Category(IntegrationTest.class)
//Commented Spring boot Test annotation, as it works only when configuration of Spring boot maven plugin goal as exec in privacy-personal-model pom.xml
//@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PersonControllerIT 
{
	//@Autowired
	//private MockMvc mockMvc;
	
	
	@Autowired
	private Environment env;
	
	private static File mavenHome = null;
	
	@Test
	public void postSensitivePII() throws Exception {
		
		System.out.println("In post Sensistive PII");
		//This option is using maven invoker to start sping boot. It is not able to find Spring boot Application class.
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File("D:/Sriram/konig-privacy/privacy-personal-model-intergation-testing/src/integration-test/resources/pom.xml"));
		request.setGoals(Collections.singletonList("spring-boot:run"));
		DefaultInvoker invoker = invoker();
		InvocationResult result = invoker.execute(request);
		assertTrue(result.getExitCode() != 0 ? result.getExecutionException().toString() : "Success",
				result.getExitCode() == 0);
		
		//below option uses sping boot annotation to invoke the service
		/*ObjectMapper mapper = new ObjectMapper();
		
		JsonNode node=mapper.readTree(new File("personRequest.json"));
		RequestBuilder requestBuilder = MockMvcRequestBuilders
				.post("/privacy/v5/person")
				.accept(MediaType.APPLICATION_JSON).content(node.toString())
				.contentType(MediaType.APPLICATION_JSON);
		
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();
		
		MockHttpServletResponse response = result.getResponse();
		
		System.out.println("Response from Service===>"+response.getStatus());
		
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());*/
		
	}
	
	private DefaultInvoker invoker() {
		DefaultInvoker invoker = new DefaultInvoker();
		File mavenHome = mavenHome();
		invoker.setMavenHome(mavenHome);
		return invoker;
	}
	
	private File mavenHome() {
		if (mavenHome == null) {
		System.out.println("Path environment"+System.getenv("PATH"));
			for (String dirname : System.getenv("PATH").split(File.pathSeparator)) {
				File file = new File(dirname, "mvn");
				if (file.isFile()) {
					mavenHome = file.getParentFile().getParentFile();
					createBatchFile();
					return mavenHome;
				}
			}
			throw new RuntimeException("Maven executable not found.");
		}
		return mavenHome;
	}
	
	private void createBatchFile() {
		
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			// We are running on a Windows machine.  Copy the file.
			File batFile = new File(mavenHome, "bin/mvn.bat");
			if (!batFile.exists()) {
				File cmdFile = new File(mavenHome, "bin/mvn.cmd");
				if (cmdFile.exists()) {
					try {
						Files.copy(cmdFile, batFile);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				
			}
		}
		
	}
    
}
