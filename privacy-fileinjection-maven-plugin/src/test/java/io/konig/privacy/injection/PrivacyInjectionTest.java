/**
 * 
 */
package io.konig.privacy.injection;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;


public class PrivacyInjectionTest {

	@Test
	public void testInjection() {
		File file = new File("src/test/resources/cloudformation.yaml");
		assertTrue(file.exists());		
		assertContainsParameters(file,"development");
		assertContainsParameters(file,"Alan Turing");
		
	}
	
	public void assertContainsParameters(File file, String inputString){
		String strResult=null;
		try {
			strResult = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(strResult.contains(inputString)){
			return;
		}
		fail("String Not Found" +inputString);
	}
}
