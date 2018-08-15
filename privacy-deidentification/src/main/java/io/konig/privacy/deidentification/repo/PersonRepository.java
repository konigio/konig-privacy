package io.konig.privacy.deidentification.repo;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.konig.privacy.deidentification.model.DatasourceData;
import io.konig.privacy.deidentification.model.DatasourceDataRowMapper;
import io.konig.privacy.deidentification.model.Identity;
import io.konig.privacy.deidentification.model.Person;
import io.konig.privacy.deidentification.model.PersonData;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonRowMapper;
import io.konig.privacy.deidentification.model.PersonWithMetadata;
import io.konig.privacy.deidentification.model.Provenance;
import net.spy.memcached.MemcachedClient;

@Repository
@Transactional
public class PersonRepository {

	final static SecureRandom secureRandom = new SecureRandom();
	private final static String URN_EMAIL = "urn:email";
	
	private static  ThreadLocal<DatasourceTrustServiceimpl> instance =new ThreadLocal<DatasourceTrustServiceimpl>();

	@Autowired
	JdbcTemplate template;
	

	@Autowired
    MemcachedClient cache;
	
	@Autowired
	private Environment env;
	
	private SimpleDateFormat isoTimestampFormat =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
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
	 * For best performance, this method ought to return a TrustService from a ThreadLocal variable that is available as a singleton in Session scope.
	 */
	private DatasourceTrustService getTrustService() {
		// TODO Auto-generated method stub
		DatasourceTrustServiceimpl trustService= new DatasourceTrustServiceimpl();
		instance.set(trustService);				
		return instance.get();
		//return trustService;		
	}

	private void doMerge(MergeInfo mergeInfo, JsonNode requestObject, ArrayNode annotations, PersonKeys keys) {
		
		ObjectMapper objectMapper = mergeInfo.getObjectMapper();
		Iterator<String> fieldNames = requestObject.fieldNames();
		String dateModifiedValue = mergeInfo.getReceivedAtTime();
		String dataSourceValue = mergeInfo.getReceivedFrom();
		
		List<String> emailList = new ArrayList<String>();
		List<Identity> identityList = new ArrayList<Identity>();
		
		List<String> diffEmailList = new ArrayList<String>();
		List<Identity> diffIdentityList = new ArrayList<Identity>();
		
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
					ObjectNode annotationNode = annotatedEmail(annotations, requestEmail);

					JsonNode emailValue = JsonNodeFactory.instance.textNode(requestEmail);
					
					if (annotationNode == null) {
						// There is no existing record of the given email address, so add a new annotated record.
						addAnnotation(mergeInfo, annotations, "email", emailValue);
						emailList.add(emailValue.asText());
						diffEmailList.add(emailValue.asText());
					} else if (overwrite(mergeInfo, annotationNode)) {
						annotationNode.set("value", emailValue);
						annotationNode.put("dateModified", dateModifiedValue);
						annotationNode.put("dataSource", dataSourceValue);
						emailList.add(emailValue.asText());
						
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

		// TODO: Save the updated record to the database.
		String updateQuery = "UPDATE DE_IDENTIFICATION.PERSON SET  ANNOTATED_PERSON_DATA=? WHERE PSEUDONYM=?";
		template.update(updateQuery, annotations.toString(), keys.getPseudonym());
		

		//This block is for adding new email and Identity elements recieved during Merge
		for(int z=0;z<ListUtils.emptyIfNull(diffEmailList).size();z++){
			String queryEmail = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
			template.update(queryEmail, keys.getPseudonym(),"urn:email" ,diffEmailList.get(z));
		}
		
		for(int y=0;y<ListUtils.emptyIfNull(diffIdentityList).size();y++){
			String queryIdentity = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
			template.update(queryIdentity, keys.getPseudonym(),diffIdentityList.get(y).getIdentityProvider() ,diffIdentityList.get(y).getIdentifier());
		}
		
		// TODO: Add direct identifiers (email and Identity objects) to the supplied PersonKeys object.
		keys.setEmail(emailList);
		keys.setIdentity(identityList);
		
		
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
			
		default:
			break;
		}
		
		throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid JSON");
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
		
		String pseudonym = randomString(30);
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


	private PersonData loadPersonData(PersonWithMetadata metaPerson) {
		StringBuilder sb=new StringBuilder("SELECT distinct a.PERSON_DATA, a.ANNOTATED_PERSON_DATA, a.PSEUDONYM");
		sb.append(" from DE_IDENTIFICATION.PERSON AS a ");
		sb.append("INNER JOIN DE_IDENTIFICATION.PERSON_IDENTITY As b ON a.PSEUDONYM= b.PERSON_PSEUDONYM  ");
		sb.append("AND ");
		
		JsonNode personNode = metaPerson.getPerson();
		
		List<String> argList = new ArrayList<>();
		
		List<Identity> arg1List = new ArrayList<>();
		//for combing the Identity and Email list
		List<Object> argFinal =new ArrayList<>();
		
		// Scan email list
		
		JsonNode emailNode = personNode.get("email");
		if (emailNode instanceof ArrayNode) {
			for (int i=0; i<emailNode.size(); i++) {
				String emailValue = emailNode.get(i).asText();
				argList.add(emailValue);
				argList.add(URN_EMAIL);
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
				arg1List.add(id);
			}
		}
		
		if (arg1List.isEmpty() && argList.isEmpty()) {
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Person record must contain at least one email or one nested identity record");
		}
		
		
		sb.append('(');
		
		
		for (int j=0; j<arg1List.size(); j++) {
			sb.append('(');
			sb.append(" b.IDENTITY_PROVIDER=?");
			sb.append(" AND b.IDENTIFIER=?");
			sb.append(")");
		}
		
		String or = "";
		for (int i=0; i<argList.size()/2; i++) {
			if(arg1List.size()>0)
				sb.append(" OR ");			
			sb.append('(');
			sb.append(" b.IDENTIFIER=?");
			sb.append(" AND b.IDENTITY_PROVIDER=?");
			sb.append(')');
		}
		
		sb.append(')');

		String sql = sb.toString();
		
		argFinal.addAll(arg1List);
		argFinal.addAll(argList);
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
	
	private static class PersonDataRowMapper implements RowMapper<PersonData> {

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
	
	

	
	

	public List<PersonKeys> put(Person person, String version) throws Exception {
		ArrayNode tagsArray = (ArrayNode) person.getPerson().findValue("data");
		String dataSourceId = person.getPerson().findValue("datasource").textValue();
		String trustValue = "";
		//if(cache.get(dataSourceId) == null) {
			//fetchDataSourceDetails(dataSourceId);
		//}
		//trustValue = cache.get(dataSourceId).toString();
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
