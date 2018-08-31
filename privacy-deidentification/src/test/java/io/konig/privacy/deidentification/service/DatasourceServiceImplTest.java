package io.konig.privacy.deidentification.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;


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

import io.konig.privacy.deidentification.model.Datasource;
import io.konig.privacy.deidentification.model.LanguageString;
import io.konig.privacy.deidentification.repo.DatasourceRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DatasourceServiceImplTest {
	
	@Autowired
    private DatasourceService datasourceService;
 
    @MockBean
    private DatasourceRepository datasourceRepository;
    
    @Before
    public void setUp() {
        Datasource datasource = new Datasource();
        List<LanguageString> nameList=new ArrayList<LanguageString>();
        List<LanguageString> descriptionList=new ArrayList<LanguageString>();
        datasource.setId("http://example.com/system/mdm");
        datasource.setUuid("PA4MV1LdSeeVeRM");
        datasource.setTrustLevel(0.80);
        
        LanguageString language= new LanguageString();
		language.setLanguage("en");
		language.setValue("meta data");
		nameList.add(language);
        datasource.setName(nameList);
        
        LanguageString description= new LanguageString();
		description.setLanguage("en");
		description.setValue("description about the data source");
		descriptionList.add(description);
		datasource.setDescription(descriptionList);
     
        Mockito.when(datasourceRepository.datasourceExists("PA4MV1LdSeeVeRM"))
          .thenReturn(true);
        
        Mockito.when(datasourceRepository.getByUuid("PA4MV1LdSeeVeRM"))
          .thenReturn(datasource);
        

    }
    
   /* @Test
    public void whenValidDatasource_thenDatasourceShouldBeFound() {
    	boolean datasourceExists=datasourceRepository.datasourceExists("PA4MV1LdSeeVeRM");
    	assertTrue(datasourceExists);
    }*/

    @Test
    public void getDatasourceById() throws DataAccessException {
    	Datasource datasource=null;
    	datasource= datasourceService.getByUuid("PA4MV1LdSeeVeRM");
    	assertNotNull(datasource);
    	assertEquals("http://example.com/system/mdm", datasource.getId());
    }
    
}
