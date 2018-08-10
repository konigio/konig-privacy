package io.konig.privacy.deidentification.repo;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;


import io.konig.privacy.deidentification.model.Identity;
import io.konig.privacy.deidentification.model.Person;
import io.konig.privacy.deidentification.model.PersonData;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonRowMapper;

@Repository
@Transactional
public class PersonRepository {

	final static SecureRandom secureRandom = new SecureRandom();

	@Autowired
	JdbcTemplate template;

	public List<PersonKeys> put(Person person, String version) throws Exception {
		ArrayNode tagsArray = (ArrayNode) person.getPerson().findValue("data");
		String dataSourceId = person.getPerson().findValue("datasource").textValue();
		List<PersonKeys> personKeyList = new ArrayList<PersonKeys>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");	
		GregorianCalendar gregoriancalendar = new GregorianCalendar();
		String dateModified=sdf.format(gregoriancalendar.getTime());
		for (int i = 0; i < tagsArray.size(); i++) {
			PersonKeys personKeys = new PersonKeys();
			PersonData personData = null;
			ObjectNode objectValue = (ObjectNode) tagsArray.get(i);
			String pseudonym = null;

			List<String> email = new ArrayList<String>();
			StringBuilder sb=new StringBuilder("SELECT distinct a.PERSON_DATA, a.ANNOTATED_PERSON_DATA, a.PSEUDONYM from DE_IDENTIFICATION.PERSON AS a ");
			sb.append("INNER JOIN DE_IDENTIFICATION.PERSON_IDENTITY As b ON a.PSEUDONYM= b.PERSON_PSEUDONYM  ");
			sb.append("AND ");
			email=fetchEmailList(objectValue, true, sb);
			personKeys.setEmail(email);

			List<Identity> identityList = new ArrayList<Identity>();
			identityList=fetchIdentityList(objectValue, true, sb);
			
			sb.append(")");
			personKeys.setIdentity(identityList);
			
			JsonNode annotedJson=createAnnotatedJson(objectValue,dataSourceId,dateModified);
			
			if(identifierExists(sb.toString())){
				PersonRowMapper personRowMapper = new PersonRowMapper();
				personData = (PersonData) template.queryForObject(sb.toString(), personRowMapper
						);
				List<String> databaseEmailList=new ArrayList<String>();
				List<Identity> databaseIdentityList = new ArrayList<Identity>();
	
				if (personData.getPerson() != null) {
					ObjectMapper om = new ObjectMapper();
					ObjectMapper annotatedObjectMapper = new ObjectMapper();
					JsonNode simpleJsonBeforeNode = om.readTree(personData.getPerson());
					JsonNode simpleJsonAfterNode = om.readTree(objectValue.toString());
					JsonNode annotatedJsonBeforeNode = annotatedObjectMapper.readTree(personData.getAnnotated_person());
					JsonNode annotatedJsonAfterNode  = annotatedObjectMapper.readTree(annotedJson.toString());
					ObjectNode node = (ObjectNode) new ObjectMapper().readTree(simpleJsonBeforeNode.toString());
					databaseEmailList= fetchEmailList(node, false, sb);
					databaseIdentityList=fetchIdentityList(node,false,sb);
					JsonNode mergeSimpleJsonNode = merge(simpleJsonBeforeNode, simpleJsonAfterNode);
					JsonNode mergeAnnotatedJsonNode = merge(annotatedJsonBeforeNode,annotatedJsonAfterNode);
					
					Collection<String> resultEmail= CollectionUtils.subtract(email, databaseEmailList);
					Collection<Identity> resultIdentity=  CollectionUtils.subtract(identityList, databaseIdentityList);
					List<String> finalEmailList =new ArrayList<String>(resultEmail);
					List<Identity> finalIdentityList = new ArrayList<Identity>(resultIdentity);
					
					String updateQuery = "UPDATE DE_IDENTIFICATION.PERSON SET PERSON_DATA=?, ANNOTATED_PERSON_DATA=? WHERE PSEUDONYM=?";
					template.update(updateQuery, mergeSimpleJsonNode.toString(), mergeAnnotatedJsonNode.toString(), personData.getPseudonym());
					
					for(int z=0;z<ListUtils.emptyIfNull(finalEmailList).size();z++){
						String queryEmail = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
						template.update(queryEmail, personData.getPseudonym(),"urn:email" ,finalEmailList.get(z));
					}
					
					for(int y=0;y<ListUtils.emptyIfNull(finalIdentityList).size();y++){
						String queryIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
						template.update(queryIdentity, personData.getPseudonym(),finalIdentityList.get(y).getIdentityProvider() ,finalIdentityList.get(y).getIdentifier());
					}
					
					
					personKeys.setPseudonym(personData.getPseudonym());
				}
			}
			else{
				pseudonym = randomString(30);
				personKeys.setPseudonym(pseudonym);
				
				String queryPerson = "INSERT INTO  DE_IDENTIFICATION.PERSON (DATASOURCE_ID, PERSON_DATA, ANNOTATED_PERSON_DATA,VERSION,PSEUDONYM) VALUES (?,?,?,?,?)";
				template.update(queryPerson, dataSourceId, objectValue.toString(), annotedJson.toString(), version, pseudonym);
				
				
				for(int k=0;k<ListUtils.emptyIfNull(email).size();k++){
					String queryPersonIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
					template.update(queryPersonIdentity, pseudonym, "urn:email" ,email.get(k));					
				}
				
				for(int l=0;l<ListUtils.emptyIfNull(identityList).size();l++){
					String queryPersonIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
					template.update(queryPersonIdentity, pseudonym, identityList.get(l).getIdentityProvider(),identityList.get(l).getIdentifier());			
				}

			}		
			personKeyList.add(personKeys);
		}

		return personKeyList;
	}

	public String randomString(int length) {
		char[] format = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
				'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
				'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
				'z' };
		String strRandomString = RandomStringUtils.random(length, 0, 62, false, false, format, secureRandom);
		return strRandomString;
	}

	public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

		Iterator<String> fieldNames = updateNode.fieldNames();

		while (fieldNames.hasNext()) {
			String updatedFieldName = fieldNames.next();
			JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
			JsonNode updatedValue = updateNode.get(updatedFieldName);

			// If the node is an @ArrayNode
			if (valueToBeUpdated != null && valueToBeUpdated.isArray() && updatedValue.isArray()) {
				// running a loop for all elements of the updated ArrayNode
				for (int i = 0; i < updatedValue.size(); i++) {
					JsonNode updatedChildNode = updatedValue.get(i);
					// Create a new Node in the node that should be updated, if
					// there was no corresponding node in it
					// Use-case - where the updateNode will have a new element
					// in its Array
					if (valueToBeUpdated.size() <= i) {
						((ArrayNode) valueToBeUpdated).add(updatedChildNode);
					}
					// getting reference for the node to be updated
					JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
					merge(childNodeToBeUpdated, updatedChildNode);
				}
				// if the Node is an @ObjectNode
			} else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
				merge(valueToBeUpdated, updatedValue);
			} else {
				if (mainNode instanceof ObjectNode) {
					((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
				}
			}
		}
		return mainNode;
	}
	
	
	public List<String> fetchEmailList(ObjectNode objectValue, boolean isDynamicQueryRequired, StringBuilder sbQuery) throws Exception{
		ArrayNode emailArray = (ArrayNode) objectValue.findValue("email");
		List<String> email = new ArrayList<String>();
			for (int j = 0; j < emailArray.size(); j++) {
				TextNode textValue = (TextNode) emailArray.get(j);
				if (textValue.textValue().isEmpty()){
					throw new Exception("Email Id cannot be empty");
				}
				if(isDynamicQueryRequired){
					if(sbQuery.indexOf("b.IDENTIFIER")>0)
						sbQuery.append(" OR b.IDENTIFIER='"+(textValue.textValue()).trim()+"'");
					else
						sbQuery.append(" (b.IDENTIFIER='"+(textValue.textValue()).trim()+"'");
					}
				email.add(textValue.textValue());			
			}
	
		return email;
	}
	
	public List<Identity> fetchIdentityList(ObjectNode objectValue, boolean isDynamicQueryRequired,StringBuilder sbQuery) throws Exception{
		ArrayNode identityArray = (ArrayNode) objectValue.findValue("identity");
		List<Identity> identityList = new ArrayList<Identity>();
			for (int z = 0; z < identityArray.size(); z++) {
				TextNode identityproviderValue = (TextNode) identityArray.get(z).get("identityProvider");
				TextNode identifier = (TextNode) identityArray.get(z).get("identifier");
				if(identityproviderValue.textValue().isEmpty() || identifier.textValue().isEmpty()){
					throw new Exception("Identifier cannot be Empty");
				}
				if(isDynamicQueryRequired){
					sbQuery.append(" OR b.IDENTIFIER='"+(identifier.textValue()).trim()+"'");
				}
				Identity identity = new Identity(identityproviderValue.textValue(), identifier.textValue());			
				identityList.add(identity);
			}				
		return identityList;
	}
	
	public boolean identifierExists(String sql) {
		String query = "SELECT COUNT(*) "+sql.substring(68);
		int count = template.queryForObject(query, Integer.class);
		if (count == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public JsonNode createAnnotatedJson(JsonNode simpleJsonNode, String datsourceId, String dateModified) throws IOException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode= mapper.readTree(simpleJsonNode.toString());
		Iterator<String> jsonNodeIterator = jsonNode.fieldNames();
		ObjectMapper annotedMapper = new ObjectMapper();
		JsonNode annotedJsonNode = annotedMapper.createObjectNode();
		ArrayNode array = mapper.createArrayNode();
		while (jsonNodeIterator.hasNext()) {
			  String itemNode = jsonNodeIterator.next();
			    JsonNode ValueNode= jsonNode.get(itemNode);
			if(!ValueNode.isArray()){
				 JsonNode childNode1 = annotedMapper.createObjectNode();
			    ((ObjectNode) childNode1).put("property", itemNode.toString());			    
			    if(ValueNode!=null && ValueNode.isTextual()){
			    	String Value=jsonNode.get(itemNode).textValue();
			    	((ObjectNode) childNode1).put("value", Value);
			    	((ObjectNode) childNode1).put("dataSource", datsourceId);
			    	((ObjectNode) childNode1).put("dateModified", dateModified);
			    	
			    	array.add(childNode1);
			    }
			}
			else{
				Iterator<JsonNode> fieldNames = ValueNode.elements();
				while(fieldNames.hasNext()){
					JsonNode childNode2 = annotedMapper.createObjectNode();
					JsonNode field = fieldNames.next();
				        	((ObjectNode) childNode2).put("property", itemNode);
				        	((ObjectNode) childNode2).set("value", field);
				        	((ObjectNode) childNode2).put("dataSource", datsourceId);
				        	((ObjectNode) childNode2).put("dateModified", dateModified);
				        	array.add(childNode2);
				}				
			}		  
		}
		((ObjectNode) annotedJsonNode).set("fields", array);
		
		return annotedJsonNode;
	}
	
	public JsonNode getAnnotatedSensitivePII(String version, String pseudonym) throws JsonProcessingException, IOException{
		String query = "SELECT ANNOTATED_PERSON_DATA from DE_IDENTIFICATION.PERSON WHERE VERSION=? and PSEUDONYM=?";
		String jsonString = template.queryForObject(query, new Object[] { version, pseudonym }, String.class);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(jsonString);
		return jsonNode;
	}
	
	public boolean annotatedPIIExists(String version, String pseudonym) {
		String query = "SELECT COUNT(*) FROM DE_IDENTIFICATION.PERSON WHERE VERSION=? and PSEUDONYM=?";
		int count = template.queryForObject(query, Integer.class, version,pseudonym);
		if (count == 0) {
			return false;
		} else {
			return true;
		}
	}
}
