package io.konig.privacy.deidentification.repo;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.konig.privacy.deidentification.model.EnvironmentConstants;
import io.konig.privacy.deidentification.model.Identity;
import io.konig.privacy.deidentification.model.PersonData;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonWithMetadata;
import io.konig.privacy.deidentification.model.Provenance;

@Repository
@Transactional
public class PersonRepository {

	final static SecureRandom secureRandom = new SecureRandom();
	private final static String URN_EMAIL = "urn:email";
	
	@Autowired 
	Environment env;

	@Autowired
	JdbcTemplate template;
	
	private String pseudonymPrefix;
	
	private SimpleDateFormat isoTimestampFormat =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	public PersonRepository() {
	}
	
	public PersonRepository(Environment env, JdbcTemplate template) {
		this.template = template;
		this.env = env;
	}
	
	

	public JdbcTemplate getJdbcTemplate() {
		return template;
	}


	public PersonKeys put(PersonWithMetadata metaPerson) throws HttpClientErrorException, IOException,Exception {
		PersonKeys keys = null;
		
		PersonData personData = loadPersonData(metaPerson);
		if (personData == null) {
			keys = insert(metaPerson);
			
		} else {
			keys = merge(metaPerson, personData);
		}
		
		
		return keys;
	}



	private PersonKeys merge(PersonWithMetadata metaPerson, PersonData personData) {
		PersonKeys keys = new PersonKeys();
		Provenance provenance = metaPerson.getMetadata().getProvenance();
		
		keys.setPseudonym(personData.getPseudonym());
		
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode requestData = metaPerson.getPerson();
		try {
			JsonNode annotationData = objectMapper.readTree(personData.getAnnotated_person());
			
			DatasourceTrustService trustService = getTrustService();
			
			String receivedAtTime = isoTimestampFormat.format(provenance.getReceivedAtTime().getTime());
			String receivedFrom = provenance.getReceivedFrom();
			
			double receivedFromTrustLevel = trustService.getTrustLevel(receivedFrom);
			
			
			MergeInfo mergeInfo = new MergeInfo(objectMapper, receivedAtTime, receivedFrom, receivedFromTrustLevel, trustService);
			
			doMerge(mergeInfo, requestData, (ArrayNode) annotationData.get("graph"), keys);
			
			
		} catch (IOException e) {
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid JSON payload");
		}
		
		return keys;
	}


	/**
	 * Get a TrustService implementation that wraps a local hash map cache, plus memcache, and ultimately defaults to a database lookup.
	 * This is a convenience method equivalent to <code>DatasourceTrustService.instance.get()<code>.
	 */
	private DatasourceTrustService getTrustService() {
		return DatasourceTrustService.instance.get();		
	}

	private void doMerge(MergeInfo mergeInfo, JsonNode requestObject, ArrayNode annotations, PersonKeys keys) {
		
		// TODO: The logic for inserting vs. updating records in the PERSON_IDENTITY table is not correct.
		// Greg will fix this later.

		ObjectMapper objectMapper = mergeInfo.getObjectMapper();
		Iterator<String> fieldNames = requestObject.fieldNames();
		String dateModifiedValue = mergeInfo.getReceivedAtTime();
		String dataSourceValue = mergeInfo.getReceivedFrom();
		
		LinkedHashSet<String> emailSet = new LinkedHashSet<>();
		LinkedHashMap<String, Identity> identityMap = new LinkedHashMap<>();
		
		List<Identity> identityList = new ArrayList<Identity>();
		
		List<String> diffEmailList = new ArrayList<String>();
		List<Identity> diffIdentityList = new ArrayList<Identity>();
		
		collectDirectIdentifiers(emailSet, identityMap, annotations);
		
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			JsonNode requestValue = requestObject.get(fieldName);
			if (fieldName.equals("identity")) {
				// Special handling for "identity" object
				
				ArrayNode identityArray = (ArrayNode) requestValue;
				for (int i=0; i<identityArray.size(); i++) {
					ObjectNode identityObject = (ObjectNode) identityArray.get(i);
					String identityProviderValue = identityObject.get("identityProvider").asText();
					String identifierValue = identityObject.get("identifier").asText();
					
					ObjectNode annotationNode = annotatedIdentity(annotations, identityProviderValue, identifierValue);
					
					if (annotationNode == null) {
						// There is no existing annotated Identity, so create a new annotated value and add it to the list.
						ObjectNode value = objectMapper.createObjectNode();
						value.put("identifier", identifierValue);
						value.put("identityProvider", identityProviderValue);
						addAnnotation(mergeInfo, annotations, "identity", value);
						Identity identity = new Identity(identityProviderValue, identifierValue);
						identityList.add(identity);
						diffIdentityList.add(identity);
						
						identityMap.put(identityProviderValue, identity);
						
					} else if (overwrite(mergeInfo, annotationNode)) {
						// There is an existing annotated Identity
						ObjectNode identityNode = (ObjectNode) annotationNode.get("value");
						identityNode.put("identifier", identifierValue);
						annotationNode.put("dateModified", dateModifiedValue);
						annotationNode.put("dataSource", dataSourceValue);
						Identity identity = new Identity(identityProviderValue, identifierValue);
						identityList.add(identity);
					}
				}
				
				
			} else if ("email".equals(fieldName)) {
				// Special handling for "email" object 
				
				ArrayNode emailArray = (ArrayNode) requestValue;
				for (int i=0; i<emailArray.size(); i++) {
					String requestEmail = emailArray.get(i).asText();
					emailSet.add(requestEmail);
					
					ObjectNode annotationNode = annotatedEmail(annotations, requestEmail);

					JsonNode emailValue = JsonNodeFactory.instance.textNode(requestEmail);
					
					if (annotationNode == null) {
						// There is no existing record of the given email address, so add a new annotated record.
						addAnnotation(mergeInfo, annotations, "email", emailValue);
						diffEmailList.add(emailValue.asText());
					} else if (overwrite(mergeInfo, annotationNode)) {
						annotationNode.set("value", emailValue);
						annotationNode.put("dateModified", dateModifiedValue);
						annotationNode.put("dataSource", dataSourceValue);
					}
				}
				
			} else {
				
				ObjectNode annotationNode = getAnnotationNode(annotations, fieldName);
				JsonNode fieldValue = copy(requestValue);
				if (annotationNode == null) {
					addAnnotation(mergeInfo, annotations, fieldName, fieldValue);
				} else if (overwrite(mergeInfo, annotationNode)){
					annotationNode.set("value", fieldValue);
					annotationNode.put("dateModified", dateModifiedValue);
					annotationNode.put("dataSource", dataSourceValue);
				}
			}
		}
		
		JdbcTemplate template = getJdbcTemplate();
		
		ObjectMapper annotedMapper = new ObjectMapper();
		JsonNode annotedJsonNode = annotedMapper.createObjectNode();
		
		((ObjectNode) annotedJsonNode).set("graph", annotations);
		
		JsonNode simpleJson=getSimpleJson(annotations);
		
		String updateQuery = "UPDATE DE_IDENTIFICATION.PERSON SET  ANNOTATED_PERSON_DATA=?, PERSON_DATA=? WHERE PSEUDONYM=?";
		template.update(updateQuery, annotedJsonNode.toString(), simpleJson.toString(), keys.getPseudonym());
		

		//This block is for adding new email and Identity elements received during Merge
		for(int z=0;z<ListUtils.emptyIfNull(diffEmailList).size();z++){
			String queryEmail = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
			template.update(queryEmail, keys.getPseudonym(),"urn:email" ,diffEmailList.get(z));
		}
		
		for(int y=0;y<ListUtils.emptyIfNull(diffIdentityList).size();y++){
			String queryIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
			template.update(queryIdentity, keys.getPseudonym(),diffIdentityList.get(y).getIdentityProvider() ,diffIdentityList.get(y).getIdentifier());
		}
		
		List<String> emailList = new ArrayList<>(emailSet);
		
		keys.setEmail(emailList);
		keys.setIdentity(identityList);
		
		
	}


	private JsonNode getSimpleJson(ArrayNode annotations) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode simpleJson=mapper.createObjectNode();
		for(int i=0;i<annotations.size();i++){
			JsonNode node=annotations.get(i);
			String propertyName=node.get("property").asText();
			JsonNode valueNode=node.get("value");
			String value=valueNode.asText();
			if("identity".equals(propertyName) || "email".equals(propertyName)){
				JsonNode identityNode=simpleJson.get(propertyName);
				if(identityNode!=null){
					if(identityNode instanceof ArrayNode){
						identityNode=((ArrayNode) identityNode).add(valueNode);
						simpleJson.set(propertyName, identityNode);
					}
					else{
						ArrayNode identityArrayNode=mapper.createArrayNode();
						identityArrayNode.add(identityNode);
						identityArrayNode.add(valueNode);
						simpleJson.set(propertyName, identityArrayNode);
					}
				}
				else{
					simpleJson.set(propertyName, valueNode);
				}
			}
			else {
				simpleJson.set(propertyName, valueNode);
			}
		}
		return simpleJson;
	}

	private void collectDirectIdentifiers(
		Set<String> emailSet,
		Map<String, Identity> identityMap, 
		ArrayNode annotations
	) {
		for (int i=0; i<annotations.size(); i++) {
			ObjectNode element = (ObjectNode) annotations.get(i);
			
			String propertyName = element.get("property").asText();
			if (propertyName.equals("email")) {
				String value = element.get("value").asText();
				emailSet.add(value);
			} else if (propertyName.equals("identity")) {
				ObjectNode node = (ObjectNode) element.get("value");
				String identifier = node.get("identifier").asText();
				String identityProvider = node.get("identityProvider").asText();
				identityMap.put(identityProvider, new Identity(identityProvider, identifier));
			}
			
			
		}
		
	}

	private JsonNode copy(JsonNode requestValue) {
		switch(requestValue.getNodeType()) {
		case BOOLEAN :
			return JsonNodeFactory.instance.booleanNode(requestValue.booleanValue());
			
		case NUMBER :
			return requestValue.canConvertToLong() ? 
				JsonNodeFactory.instance.numberNode(requestValue.longValue()) :
				JsonNodeFactory.instance.numberNode(requestValue.doubleValue());
				
		case STRING :
			return JsonNodeFactory.instance.textNode(requestValue.asText());
			
		case OBJECT :
			return copyObject((ObjectNode) requestValue);
			
		case ARRAY :
			return copyArray((ArrayNode)requestValue);
			
		default:
			break;
		}
		
		throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid JSON");
	}



	private JsonNode copyArray(ArrayNode array) {
		ArrayNode copy = JsonNodeFactory.instance.arrayNode();
		for (int i=0; i<array.size(); i++) {
			JsonNode element = array.get(i);
			copy.add(copy(element));
		}
		
		return copy;
	}



	private JsonNode copyObject(ObjectNode object) {
		ObjectNode copy = JsonNodeFactory.instance.objectNode();
		Iterator<String> sequence = object.fieldNames();
		while (sequence.hasNext()) {
			String fieldName = sequence.next();
			JsonNode fieldValue = object.get(fieldName);
			JsonNode fieldValueCopy = copy(fieldValue);
			copy.set(fieldName, fieldValueCopy);
		}
		
		return copy;
	}



	private ObjectNode getAnnotationNode(ArrayNode annotations, String fieldName) {
		for (int i=0; i<annotations.size(); i++) {
			JsonNode node = annotations.get(i);
			String propertyName = node.get("property").asText();
			if (propertyName.equals(fieldName)) {
				return (ObjectNode) node;
			}
		}
		return null;
	}



	/**
	 * Search a given array of annotations for a JSON Object whose "property" field has the value "email"
	 * and whose "value" field matches the given <code>requestEmail</code>
	 * @param annotations  The array of existing annotations.
	 * @param requestEmail The email address to be matched.
	 * @return The ObjectNode for the specified requestEmail, or null if no such node is found.
	 */
	private ObjectNode annotatedEmail(ArrayNode annotations, String requestEmail) {
		// TODO Auto-generated method stub		
		for (int i=0; i<annotations.size(); i++) {
			JsonNode node = annotations.get(i);
			String propertyName = node.get("property").asText();
			String emailValue= node.get("value").asText();
			if ("email".equals(propertyName) && emailValue.equals(requestEmail)) {	
				return (ObjectNode) node;
			}
		}
		return null;		
	}

	private boolean overwrite(MergeInfo mergeInfo, JsonNode annotationValue) {
		String receivedFrom = mergeInfo.getReceivedFrom();
		String dataSource = annotationValue.get("dataSource").asText();
		if (receivedFrom.equals(dataSource)) {
			return true;
		}
		
		double dataSourceTrustLevel = mergeInfo.getTrustService().getTrustLevel(dataSource);
		double receivedFromTrustLevel = mergeInfo.getReceivedFromTrustLevel();
		
		return receivedFromTrustLevel > dataSourceTrustLevel;
	}

	private void addAnnotation(MergeInfo mergeInfo, ArrayNode annotationList, String fieldName, JsonNode fieldValue) {
		ObjectMapper mapper = mergeInfo.getObjectMapper();
		
		ObjectNode annotation = mapper.createObjectNode();
		annotation.put("property", fieldName);
		annotation.set("value", fieldValue);
		annotation.put("dateModified", mergeInfo.getReceivedAtTime());
		annotation.put("dataSource", mergeInfo.getReceivedFrom());
		
		annotationList.add(annotation);
		
		
	}


	/**
	 * Search a given array of annotations for a JSON Object whose "property" field has the value "identity"
	 * and whose "value" field is an identity object where the identityProvider and identifier match the supplied values.
	 */
	private ObjectNode annotatedIdentity(JsonNode annotations, String identityProvider, String identifier) {
		// TODO Auto-generated method stub
		for (int i=0; i<annotations.size(); i++) {
			JsonNode node = annotations.get(i);
			String propertyName = node.get("property").asText();
			if("identity".equals(propertyName) &&  node instanceof ObjectNode){
				String identityProviderValue= node.get("value").get("identityProvider").asText();
				String identifierValue = node.get("value").get("identifier").asText();
				if ("identity".equals(propertyName) && identityProviderValue.equals(identityProvider) &&  identifierValue.equals(identifier)) {
					return (ObjectNode) node;
				}
			}
		}
		
		return null;
	}


	/**
	 * Insert a new record for the given person.
	 * @throws Exception 
	 */
	private PersonKeys insert(PersonWithMetadata metaPerson) throws Exception {
		// TODO Auto-generated method stub
		
		String pseudonym = generatePseudonym();
		PersonKeys keys=new PersonKeys();
		keys.setPseudonym(pseudonym);
		String dataSourceId = metaPerson.getMetadata().getProvenance().getReceivedFrom();
		String receivedAtTime = isoTimestampFormat.format(metaPerson.getMetadata().getProvenance().getReceivedAtTime().getTime());
		JsonNode annotedJson=createAnnotatedJson(metaPerson.getPerson(),dataSourceId,receivedAtTime);
		String queryPerson = "INSERT INTO  DE_IDENTIFICATION.PERSON (DATASOURCE_ID, PERSON_DATA, ANNOTATED_PERSON_DATA,VERSION,PSEUDONYM) VALUES (?,?,?,?,?)";
		template.update(queryPerson, dataSourceId, metaPerson.getPerson().toString(), annotedJson.toString(), metaPerson.getMetadata().getDataModel().getVersion(), pseudonym);
		
		List<String> emailList = new ArrayList<>();
		List<Identity> identityList = new ArrayList<>();
		JsonNode emailNode = metaPerson.getPerson().get("email");
		if (emailNode instanceof ArrayNode)
			emailList=getEmailList(metaPerson.getPerson());
		JsonNode identityNodeList = metaPerson.getPerson().get("identity");
		if(identityNodeList instanceof ArrayNode)
			identityList=getIdentityList(metaPerson.getPerson());
		
		//Storing the Identifiers in the DB
		for(int k=0;k<ListUtils.emptyIfNull(emailList).size();k++){
			String queryPersonIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
			template.update(queryPersonIdentity, pseudonym, URN_EMAIL ,emailList.get(k));					
		}
		for(int l=0;l<ListUtils.emptyIfNull(identityList).size();l++){
			String queryPersonIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
			template.update(queryPersonIdentity, pseudonym, identityList.get(l).getIdentityProvider(),identityList.get(l).getIdentifier());			
		}
		
		
		keys.setEmail(emailList);
		keys.setIdentity(identityList);;
		return keys;
	}
	
	private String pseudonymPrefix() {
		if (pseudonymPrefix == null) {
			String name = env.getProperty(EnvironmentConstants.SERVICE_INSTANCE_ID);
			if (name == null) {
				throw new HttpServerErrorException(
						HttpStatus.INTERNAL_SERVER_ERROR, 
						EnvironmentConstants.SERVICE_INSTANCE_ID +" configuration parameter is not defined");
			}
			pseudonymPrefix = name + "-";
		}
		return pseudonymPrefix;
	}


	private String generatePseudonym() {
		String random = randomString(30);
		return pseudonymPrefix() + random;
	}

	private PersonData loadPersonData(PersonWithMetadata metaPerson) {
		StringBuilder sb=new StringBuilder("SELECT distinct a.PERSON_DATA, a.ANNOTATED_PERSON_DATA, a.PSEUDONYM");
		sb.append(" from DE_IDENTIFICATION.PERSON AS a ");
		sb.append("INNER JOIN DE_IDENTIFICATION.PERSON_IDENTITY As b ON a.PSEUDONYM= b.PERSON_PSEUDONYM  ");
		sb.append("AND ");
		
		JsonNode personNode = metaPerson.getPerson();
		
		List<String> argList = new ArrayList<>();
		
		List<String> arg1List = new ArrayList<>();
		//for combing the Identity and Email list
		List<String> argFinal =new ArrayList<>();
		
		// Scan email list
		
		JsonNode emailNode = personNode.get("email");
		if (emailNode instanceof ArrayNode) {
			for (int i=0; i<emailNode.size(); i++) {
				String emailValue = emailNode.get(i).asText();				
				argList.add(URN_EMAIL);
				argList.add(emailValue);
			}
		}
		
		
		// Scan identity list
		JsonNode identityList = personNode.get("identity");
		if (identityList instanceof ArrayNode) {
			for (int i=0; i<identityList.size(); i++) {
				JsonNode identity = identityList.get(i);
				String identifier = identity.get("identifier").asText();
				String identityProvider = identity.get("identityProvider").asText();
				Identity id= new Identity(identityProvider,identifier);
				arg1List.add(identityProvider);
				arg1List.add(identifier);
			}
		}
		
		if (arg1List.isEmpty() && argList.isEmpty()) {
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Person record must contain at least one email or one nested identity record");
		}
		
		argFinal.addAll(arg1List);
		argFinal.addAll(argList);
		
		sb.append('(');
				
		
		String or = "";
		for (int i=0; i<argFinal.size()/2; i++) {
			sb.append(or);
			or = " OR ";		
			sb.append('(');
			sb.append(" b.IDENTITY_PROVIDER=?");
			sb.append(" AND b.IDENTIFIER=?");
			sb.append(')');
		}
		
		sb.append(')');

		String sql = sb.toString();
		
		Object[] args = argFinal.toArray();
		int[] argTypes = new int[args.length];		
		Arrays.fill(argTypes, java.sql.Types.VARCHAR);
		
		
		RowMapper<PersonData> rowMapper = new PersonDataRowMapper();
	
		List<PersonData> pojoList = template.query(sql, args, argTypes, rowMapper);
		
									
		return pojoList.isEmpty() ? null : pojoList.get(0);
	}
	
	public List<String> getEmailList(JsonNode personNode) throws Exception{
		JsonNode emailNode = personNode.get("email");
		List<String> email = new ArrayList<String>();
			for (int j = 0; j < emailNode.size(); j++) {
				String emailValue = emailNode.get(j).asText();
				if (emailValue.isEmpty()){
					throw new Exception("Email Id cannot be empty");
				}
				email.add(emailValue);			
			}	
		return email;
	}
	
	public List<Identity> getIdentityList(JsonNode personNode) throws Exception{
		JsonNode identityNode = personNode.get("identity");
		List<Identity> identityList = new ArrayList<Identity>();
			for (int z = 0; z < identityNode.size(); z++) {
				JsonNode identityItem = identityNode.get(z);
				String identifier = identityItem.get("identifier").asText();
				String identityProvider = identityItem.get("identityProvider").asText();
				if(identityProvider.isEmpty() || identifier.isEmpty()){
					throw new Exception("Identifier cannot be Empty");
				}				
				Identity identity = new Identity(identityProvider, identifier);			
				identityList.add(identity);
			}				
		return identityList;
	}
	
	static class PersonDataRowMapper implements RowMapper<PersonData> {

		@Override
		public PersonData mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			String pseudonym = resultSet.getString("PSEUDONYM");
			String personData = resultSet.getString("PERSON_DATA");
			String annotatedPersonData = resultSet.getString("ANNOTATED_PERSON_DATA");
			
			PersonData result = new PersonData();
			result.setPseudonym(pseudonym);
			result.setPerson(personData);
			result.setAnnotated_person(annotatedPersonData);
			
			
			return result;
		}
		
	}
	
	private static class MergeInfo {
		private ObjectMapper objectMapper;
		private String receivedAtTime;
		private String receivedFrom;
		private double receivedFromTrustLevel;
		private DatasourceTrustService trustService;
		
		
		

		public MergeInfo(
				ObjectMapper objectMapper, String receivedAtTime, String receivedFrom, double receivedFromTrustLevel,
				DatasourceTrustService trustService) {
			super();
			this.objectMapper = objectMapper;
			this.receivedAtTime = receivedAtTime;
			this.receivedFrom = receivedFrom;
			this.receivedFromTrustLevel = receivedFromTrustLevel;
			this.trustService = trustService;
		}




		public double getReceivedFromTrustLevel() {
			return receivedFromTrustLevel;
		}




		public String getReceivedAtTime() {
			return receivedAtTime;
		}




		public String getReceivedFrom() {
			return receivedFrom;
		}




		public DatasourceTrustService getTrustService() {
			return trustService;
		}




		public ObjectMapper getObjectMapper() {
			return objectMapper;
		}

		
		
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
		((ObjectNode) annotedJsonNode).set("graph", array);
		
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
	
	public  double fetchDataSourceDetails(String id){
		String query="SELECT TRUST_LEVEL FROM DE_IDENTIFICATION.DATASOURCE where ID=?";
		double trustLevel= template.queryForObject(query, double.class,id);
		//cache.set(datasourceData.getId(), Integer.parseInt(env.getProperty("aws.memcache.expirytime")), datasourceData.getTrustLevel());
		return trustLevel;
	}

}
