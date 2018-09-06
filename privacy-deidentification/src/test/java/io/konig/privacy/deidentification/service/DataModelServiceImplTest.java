package io.konig.privacy.deidentification.service;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.konig.privacy.deidentification.repo.DataModelRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DataModelServiceImplTest {

	@Autowired
    private DataModelService dataModelService;
 
    @MockBean
    private DataModelRepository dataModelRepository;
    
    @Before
    public void setUp() throws JsonProcessingException, IOException, DataAccessException {
    	String jsonStr = "{\n "+
   "  \"title\" : \"Person\", \n" +
   "  \"type\" : \"object\", \n" +
   "  \"properties\" : { \n" +
   "  \"givenName\" : { \n" +
   "   \"type \" : \"string\" \n" +
   "   }, \n" +
   "  \"familyName\" : { \n" +
   "  \"type\" : \"string\" \n" +
   "  }, \n" +
   "  \"email\" : { \n" +
   "  \"type \" : \"array\", \n"+
   "  \"items \" : { \n" +
   "  \"type\" : \"string\" \n" +
   "  } \n" +
   "  } \n" +
   "  } \n" +
   "  } \n";
    
    	ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readValue(jsonStr, JsonNode.class);
		
		
		 Mockito.when(dataModelRepository.getSchemaByVersion("V1"))
         .thenReturn(jsonNode);
		 
		 Mockito.when(dataModelRepository.getLatestDataModel())
         .thenReturn("V1");
		 
		 Mockito.when(dataModelRepository.dataModelExists("V1"))
		 .thenReturn(true);
    }
    
    @Test
    public void getSchemaByVersion() throws DataAccessException, JsonProcessingException, IOException {
    	JsonNode jsonNode= dataModelService.getSchemaByVersion("V1");
    	assertNotNull(jsonNode);
    	assertEquals("Person",jsonNode.path("title").textValue());    	
    }
    
    @Test
    public void getlatestDataModel() throws DataAccessException, JsonProcessingException, IOException {
    	String version= dataModelService.getLatestDataModel();
    	assertNotNull(version);
    	assertEquals("V1",version);
    }
}
