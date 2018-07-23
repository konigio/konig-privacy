package io.konig.privacy.deidentification.repo;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import io.konig.privacy.deidentification.model.Identity;
import io.konig.privacy.deidentification.model.Person;
import io.konig.privacy.deidentification.model.PersonData;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonRowMapper;

@Repository
@Transactional
public class PersonRepository {

	final static SecureRandom secureRandom = new SecureRandom();

	private static final String FETCH_QUERY = "SELECT DE_IDENTIFICATION.PERSON.PERSON_DATA, DE_IDENTIFICATION.PERSON.PSEUDONYM from "
			+ "DE_IDENTIFICATION.PERSON  INNER JOIN DE_IDENTIFICATION.PERSON_EMAIL ON DE_IDENTIFICATION.PERSON.PSEUDONYM= DE_IDENTIFICATION.PERSON_EMAIL.PERSON_PSEUDONYM "
			+ "AND trim(DE_IDENTIFICATION.PERSON_EMAIL.EMAIL_ID)=?";

	@Autowired
	JdbcTemplate template;

	public List<PersonKeys> put(Person person, String version, String baseURL) throws ProcessingException, IOException {
		ArrayNode tagsArray = (ArrayNode) person.getPerson().findValue("data");
		String dataSourceId = person.getPerson().findValue("datasource").textValue();
		List<PersonKeys> personKeyList = new ArrayList<PersonKeys>();
		for (int i = 0; i < tagsArray.size(); i++) {
			PersonKeys personKeys = new PersonKeys();
			PersonData personData = null;
			ObjectNode objectValue = (ObjectNode) tagsArray.get(i);
			String pseudonym = null;
			ArrayNode emailArray = (ArrayNode) objectValue.findValue("email");
			List<String> email = new ArrayList<String>();
			boolean IdentifierExists = false;
			for (int j = 0; j < emailArray.size(); j++) {
				TextNode textValue = (TextNode) emailArray.get(j);
				IdentifierExists = identifierExists(textValue.textValue());
				if (IdentifierExists) {

					PersonRowMapper personRowMapper = new PersonRowMapper();
					personData = (PersonData) template.queryForObject(FETCH_QUERY, personRowMapper,
							textValue.textValue());

					if (personData.getPerson() != null) {
						ObjectMapper om = new ObjectMapper();
						JsonNode beforeNode = om.readTree(personData.getPerson());
						JsonNode afterNode = om.readTree(objectValue.toString());
						JsonNode mergeNode = merge(beforeNode, afterNode);

						String updateQuery = "UPDATE DE_IDENTIFICATION.PERSON SET PERSON_DATA=? WHERE PSEUDONYM=?";
						template.update(updateQuery, mergeNode.toString(), personData.getPseudonym());

						personKeys.setId(baseURL + personData.getPseudonym());
						personKeys.setPseudonym(personData.getPseudonym());
					}
				} else {
					pseudonym = randomString(30);
					personKeys.setId(baseURL + pseudonym);
					personKeys.setPseudonym(pseudonym);

					String query = "INSERT INTO  DE_IDENTIFICATION.PERSON (DATASOURCE_ID, PERSON_DATA, VERSION,PSEUDONYM) VALUES (?,?,?,?)";
					template.update(query, dataSourceId, objectValue.toString(), version, pseudonym);

					String query1 = "INSERT INTO  DE_IDENTIFICATION.PERSON_EMAIL (PERSON_PSEUDONYM, EMAIL_ID) VALUES (?,?)";
					template.update(query1, pseudonym, textValue.textValue());
				}
				email.add(textValue.textValue());
			}
			personKeys.setEmail(email);
			// TODO Need to check how to insert missing Identifier when the
			// Email is already available.
			ArrayNode identityArray = (ArrayNode) objectValue.findValue("identity");
			List<Identity> identityList = new ArrayList<Identity>();
			for (int z = 0; z < identityArray.size(); z++) {
				TextNode identityproviderValue = (TextNode) identityArray.get(z).get("identityProvider");
				TextNode identifier = (TextNode) identityArray.get(z).get("identifier");
				Identity identity = new Identity(identityproviderValue.textValue(), identifier.textValue());
				if (!IdentifierExists) {
					String query2 = "INSERT INTO  DE_IDENTIFICATION.PERSON_IDENTITY (PERSON_PSEUDONYM, IDENTITY_PROVIDER,IDENTIFIER) VALUES (?,?,?)";
					template.update(query2, pseudonym, identityproviderValue.textValue(), identifier.textValue());
				}
				identityList.add(identity);
			}
			personKeys.setIdentity(identityList);
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

	public boolean identifierExists(String id) {
		String query = "SELECT COUNT(*) FROM DE_IDENTIFICATION.PERSON_EMAIL WHERE EMAIL_ID=?";
		int count = template.queryForObject(query, Integer.class, id);
		if (count == 0) {
			return false;
		} else {
			return true;
		}
	}
}
