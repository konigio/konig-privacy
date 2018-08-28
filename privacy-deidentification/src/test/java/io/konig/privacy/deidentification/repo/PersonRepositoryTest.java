package io.konig.privacy.deidentification.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.konig.privacy.deidentification.model.DataModel;
import io.konig.privacy.deidentification.model.EnvironmentConstants;
import io.konig.privacy.deidentification.model.Identity;
import io.konig.privacy.deidentification.model.Metadata;
import io.konig.privacy.deidentification.model.PersonData;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonWithMetadata;
import io.konig.privacy.deidentification.model.Provenance;
import io.konig.privacy.deidentification.repo.PersonRepository.PersonDataRowMapper;

public class PersonRepositoryTest {	
	@Mock
	JdbcTemplate jdbcTemplate;
	
	@Mock 
	DatasourceTrustService datasourceTrustService;
	
	private PersonRepository repository;
	private ObjectNode personJson;
	private PersonWithMetadata personWithMetadata;
	private String trustedDatasource = "http://trustedDatasource.com/";
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		
		DatasourceTrustService.instance.set(datasourceTrustService);
		MockEnvironment env = new MockEnvironment();
		env.setProperty(EnvironmentConstants.SERVICE_INSTANCE_ID, "junit");
		
		repository = new PersonRepository(env, jdbcTemplate);

		personJson = loadJson("PersonRepositoryTest/personRequest.json");

		personWithMetadata = new PersonWithMetadata();

		Metadata metadata = new Metadata();
		Provenance prov = new Provenance();
		metadata.setProvenance(prov);
		
		DataModel dataModel = new DataModel();
		dataModel.setVersion("v1");
		
		metadata.setDataModel(dataModel);
		
		GregorianCalendar receivedAtTime = new GregorianCalendar(2018, 7, 1, 10, 30);
		
		prov.setReceivedAtTime(receivedAtTime);
		prov.setReceivedFrom(trustedDatasource);
		
		personWithMetadata.setPerson(personJson);
		personWithMetadata.setMetadata(metadata);
		

		
		// Define responses from the Mock DatasourceTrustService
		
		when(datasourceTrustService.getTrustLevel(trustedDatasource)).thenReturn(0.9);
		when(datasourceTrustService.getTrustLevel("http://example.org/untrustedDatasource.com/")).thenReturn(0.1);
	}
	
	private ObjectNode loadJson(String path) throws Exception {
		InputStream input = getClass().getClassLoader().getResourceAsStream(path);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree(input);
		input.close();

		return node;
	}

	@Ignore
	public void testInsert() throws Exception {
		
		

		List<PersonData> personDataList = new ArrayList<>();

		// Define responses from the mock JdbcTemplate
		
		when(jdbcTemplate.query(any(String.class), any(Object[].class), any(int[].class), any(PersonDataRowMapper.class))).thenReturn(personDataList);
		

		PersonKeys personKeys = repository.put(personWithMetadata);

		// Verify that the generated pseudonym has the expected prefix
		
		String pseudonym = personKeys.getPseudonym();
		assertTrue(pseudonym.startsWith("junit-"));
		
		// TODO: Verify that the SQL update statements were executed correctly
		
	}

	@Test
	public void testMerge() throws Exception {
		
		List<PersonData> personDataList = new ArrayList<>();
		
		String pseudonymExpected = "expectedPseudonym";
		String priorSimplePerson = loadAsString("PersonRepositoryTest/priorSimplePerson.json");
		
		
		String priorAnnotatedPerson =
			"{\n" + 
			"  \"graph\" : [{\n" + 
			"    \"property\" : \"email\",\n" + 
			"    \"value\" : \"alice.jones@example.com\",\n" + 
			"    \"dateModified\" : \"2018-01-01T08:00.000Z\",\n" + 
			"    \"dataSource\" : \"http://untrustedDatasource.com/\"\n" + 
			"  }]\n" + 
			"}";

		
		PersonData priorPersonData = new PersonData();
		priorPersonData.setPseudonym(pseudonymExpected);
		priorPersonData.setPerson(priorSimplePerson);
		priorPersonData.setAnnotated_person(priorAnnotatedPerson);
		
		personDataList.add(priorPersonData);
		
		// Define responses from the mock JdbcTemplate
		
		when(jdbcTemplate.query(any(String.class), any(Object[].class), any(int[].class), any(PersonDataRowMapper.class))).thenReturn(personDataList);
		
		
		PersonKeys personKeys = repository.put(personWithMetadata);
		
		// Verify the personKeys returned by the repository

		assertEquals(pseudonymExpected, personKeys.getPseudonym());
		
		assertTrue(personKeys.getEmail().contains("alice.jones@example.com"));
		assertTrue(personKeys.getEmail().contains("alice@example.com"));
		
		List<Identity> identityList = personKeys.getIdentity();
		assertTrue(identityList != null);
		assertEquals(2, identityList.size());
		
		Identity identity = identityList.get(0);
		assertEquals("alice.jones", identity.getIdentifier());
		assertEquals("http://firstIdentityProvider.com", identity.getIdentityProvider());
		
		identity = identityList.get(1);
		assertEquals("ajones", identity.getIdentifier());
		assertEquals("http://secondIdentityProvider.com", identity.getIdentityProvider());
		
		ArgumentCaptor<String> updateSqlCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> updateArgCaptor = ArgumentCaptor.forClass(Object.class);
		
		// Verify that the PersonRepository executed three SQL update statements
		
		verify(jdbcTemplate, times(4)).update(updateSqlCaptor.capture(), updateArgCaptor.capture());
		
		List<String> actualUpdateSql = updateSqlCaptor.getAllValues();
		
		// Verify that the first update statement was executed correctly.
		
		String firstUpdateSqlExpected = "UPDATE DE_IDENTIFICATION.PERSON SET  ANNOTATED_PERSON_DATA=? WHERE PSEUDONYM=?";
		
		assertEquals(firstUpdateSqlExpected, actualUpdateSql.get(0));
		
		List<Object> updateArgList = updateArgCaptor.getAllValues();
		assertEquals(11, updateArgList.size());
		
		String annotatedPersonExpected = loadAsString("PersonRepositoryTest/expectedAnnotatedPerson.json");
		
		assertEquals(annotatedPersonExpected, updateArgList.get(0));
		
		assertEquals(pseudonymExpected, updateArgList.get(1));

		// Verify that the second update statement was executed correctly
		String insertPersonIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
		assertEquals(insertPersonIdentity, actualUpdateSql.get(1));
		
		assertEquals(pseudonymExpected, updateArgList.get(2));
		assertEquals("urn:email", updateArgList.get(3));
		assertEquals("alice@example.com", updateArgList.get(4));
		
		// Verify that the third update statement was executed correctly

		assertEquals(insertPersonIdentity, actualUpdateSql.get(2));
		assertEquals(pseudonymExpected, updateArgList.get(5));
		assertEquals("http://firstIdentityProvider.com", updateArgList.get(6));
		assertEquals("alice.jones", updateArgList.get(7));
		
		// Verify that the fourth update statement was executed correctly

		assertEquals(insertPersonIdentity, actualUpdateSql.get(3));
		assertEquals(pseudonymExpected, updateArgList.get(8));
		assertEquals("http://secondIdentityProvider.com", updateArgList.get(9));
		assertEquals("ajones", updateArgList.get(10));
		
		
		
	}
	
	@Test
	public void testMergeWithArrayOfNestedObject() throws Exception {
		List<PersonData> personDataList = new ArrayList<>();
		
		String pseudonymExpected = "expectedPseudonym";
		String priorSimplePerson = loadAsString("PersonRepositoryTest/priorSimplePersonWithArrayOfNestedObject.json");
		
		
		String priorAnnotatedPerson =
			"{\n" + 
			"  \"graph\" : [{\n" + 
			"    \"property\" : \"email\",\n" + 
			"    \"value\" : \"alice.jones@example.com\",\n" + 
			"    \"dateModified\" : \"2018-01-01T08:00.000Z\",\n" + 
			"    \"dataSource\" : \"http://untrustedDatasource.com/\"\n" + 
			"  },{\n" + 
			"    \"property\" : \"postalAddress\",\n" + 
			"    \"value\" : [{\n"+
			"	\"addressLocality\" : \"Seattle\",\n" +
			"	\"streetAddress\" : \"20 Downstreet\",\n" +
			"	\"postalCode\" : \"600123\"\n"+
			"	},{\n"+
			"	\"streetAddress\" : \"38 avenue de l'Opera\",\n" +
			"	\"postalCode\" : \"331468\",\n"+
			"	\"addressLocality\" : \"Paris, France\"\n"+
			"	}],\n"+
			"    \"dateModified\" : \"2018-01-01T08:00.000Z\",\n" + 
			"    \"dataSource\" : \"http://untrustedDatasource.com/\"\n" + 
			"  }]\n" + 
			"}";

		
		PersonData priorPersonData = new PersonData();
		priorPersonData.setPseudonym(pseudonymExpected);
		priorPersonData.setPerson(priorSimplePerson);
		priorPersonData.setAnnotated_person(priorAnnotatedPerson);
		
		personDataList.add(priorPersonData);
		
		when(jdbcTemplate.query(any(String.class), any(Object[].class), any(int[].class), any(PersonDataRowMapper.class))).thenReturn(personDataList);
		
		personJson = loadJson("PersonRepositoryTest/personRequestWithNestedObject.json");
		personWithMetadata.setPerson(personJson);
		PersonKeys personKeys = repository.put(personWithMetadata);
		ArgumentCaptor<String> updateSqlCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> updateArgCaptor = ArgumentCaptor.forClass(Object.class);
		
		verify(jdbcTemplate, times(4)).update(updateSqlCaptor.capture(), updateArgCaptor.capture());
		
		List<String> updateSql = updateSqlCaptor.getAllValues();
		
		String sqlExpected = "UPDATE DE_IDENTIFICATION.PERSON SET  ANNOTATED_PERSON_DATA=? WHERE PSEUDONYM=?";
		
		assertEquals(sqlExpected, updateSql.get(0));
		
		List<Object> updateArgList = updateArgCaptor.getAllValues();
		
		String annotatedPersonExpected = loadAsString("PersonRepositoryTest/expectedAnnotatedPersonWithNestedObject.json");
		
		assertEquals(annotatedPersonExpected, updateArgList.get(0));
	}
	
	
	@Test
	public void testMergeWithNestedObject() throws Exception {
		List<PersonData> personDataList = new ArrayList<>();
		
		String pseudonymExpected = "expectedPseudonym";
		String priorSimplePerson = loadAsString("PersonRepositoryTest/priorSimplePersonWithNestedObject.json");
		
		
		String priorAnnotatedPerson =
			"{\n" + 
			"  \"graph\" : [{\n" + 
			"    \"property\" : \"email\",\n" + 
			"    \"value\" : \"alice.jones@example.com\",\n" + 
			"    \"dateModified\" : \"2018-01-01T08:00.000Z\",\n" + 
			"    \"dataSource\" : \"http://untrustedDatasource.com/\"\n" + 
			"  },{\n" + 
			"    \"property\" : \"contactPoint\",\n" + 
			"    \"value\" : {\n"+
			"	\"telephone\" : \"+1-877-746-0909\",\n" +
			"	\"contactType\" : \"customer service\",\n" +
			"	\"contactOption\" : \"TollFree\",\n"+
			"	\"areaServed\" : \"US\"\n"+
			"	},\n"+
			"    \"dateModified\" : \"2018-01-01T08:00.000Z\",\n" + 
			"    \"dataSource\" : \"http://untrustedDatasource.com/\"\n" + 
			"  }]\n" + 
			"}";

		
		PersonData priorPersonData = new PersonData();
		priorPersonData.setPseudonym(pseudonymExpected);
		priorPersonData.setPerson(priorSimplePerson);
		priorPersonData.setAnnotated_person(priorAnnotatedPerson);
		
		personDataList.add(priorPersonData);
		
		when(jdbcTemplate.query(any(String.class), any(Object[].class), any(int[].class), any(PersonDataRowMapper.class))).thenReturn(personDataList);
		
		personJson = loadJson("PersonRepositoryTest/personRequestWithNestedObject1.json");
		personWithMetadata.setPerson(personJson);
		PersonKeys personKeys = repository.put(personWithMetadata);
		ArgumentCaptor<String> updateSqlCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> updateArgCaptor = ArgumentCaptor.forClass(Object.class);
		
		verify(jdbcTemplate, times(4)).update(updateSqlCaptor.capture(), updateArgCaptor.capture());
		
		List<String> updateSql = updateSqlCaptor.getAllValues();
		
		String sqlExpected = "UPDATE DE_IDENTIFICATION.PERSON SET  ANNOTATED_PERSON_DATA=? WHERE PSEUDONYM=?";
		
		assertEquals(sqlExpected, updateSql.get(0));
		
		List<Object> updateArgList = updateArgCaptor.getAllValues();
		
		String annotatedPersonExpected = loadAsString("PersonRepositoryTest/expectedAnnotatedPersonWithNestedObject1.json");
		
		assertEquals(annotatedPersonExpected, updateArgList.get(0));
	}
	
	private String loadAsString(String path) {
		InputStream input = getClass().getClassLoader().getResourceAsStream(path);
		if (input == null) {
			throw new RuntimeException("Resource not found: " + path);
		}
		Scanner scanner = new Scanner(input, "UTF-8");
		scanner.useDelimiter("\\A");
		String result = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		return result;
		
		
	}
}
