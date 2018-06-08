/**
 * 
 */
package io.konig.privacy.injection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class PrivacyInjectionTest extends AbstractMojoTestCase {
	

	@Test
	public void testInjection() throws Exception {
		File pom = getTestFile("src/test/resources/pom.xml");		
		assertNotNull(pom);
		assertTrue(pom.exists());
		PrivacyInjectionMojo myMojo= (PrivacyInjectionMojo) lookupMojo("inject", pom);
		assertNotNull(myMojo);
		myMojo.execute();		
		String workingdir=System.getProperty("user.dir");
		File file=new File(workingdir+"/target/deploy/aws/cloudformation.yaml");
		assertNotNull(file);
		assertTrue(file.exists());
		YAMLFactory yf = new YAMLFactory();
		ObjectMapper mapper = new ObjectMapper(yf);
		ObjectNode root = (ObjectNode) mapper.readTree(file);
		JsonNode tagsNode=root.path("Resources").path("VPC");
		ArrayNode tagsArray = (ArrayNode)tagsNode.findValue("Tags");
		List<String> InjectionList= new ArrayList<String>();		
		for(int i=0;i<tagsArray.size();i++){
			ObjectNode objectValue = (ObjectNode) tagsArray.get(i);
			InjectionList.add(objectValue.findValue("Value").textValue());
		}
		assertContainsParameters(InjectionList,"foo");
		assertContainsParameters(InjectionList,"Alan Turing");
	}
	
	public void assertContainsParameters(List<String> injectionList , String inputString){		
		if(injectionList.contains(inputString)){
			return;
		}
		fail("String Not Found" +inputString);
	}
		
}
