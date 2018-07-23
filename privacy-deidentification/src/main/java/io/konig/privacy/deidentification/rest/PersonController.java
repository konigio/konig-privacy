package io.konig.privacy.deidentification.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.konig.privacy.deidentification.model.Person;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.service.DataModelService;
import io.konig.privacy.deidentification.service.PersonService;
import io.konig.privacy.deidentification.utils.ValidationUtils;

@RestController
@RequestMapping(value = { "/api" })

public class PersonController {

	@Autowired
	PersonService personService;

	@Autowired
	DataModelService dataModelService;

	@Autowired
	private Environment env;

	@RequestMapping(value = "/privacy/{version}/person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> postSensitivePII(@PathVariable("version") String version, @RequestBody String strBody)
			throws Exception {
		List<PersonKeys> personKeyList = new ArrayList<PersonKeys>();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(strBody);
		String baseURL = env.getProperty("baseURL");
		Person person = new Person();
		person.setPerson(actualObj);
		ArrayNode tagsArray = (ArrayNode) actualObj.findValue("data");
		String tempVersion = version.substring(1);
		JsonNode jsonSchema = dataModelService.getSchemaByVersion(tempVersion);
		for (int i = 0; i < tagsArray.size(); i++) {
			ObjectNode objectValue = (ObjectNode) tagsArray.get(i);
			if (!ValidationUtils.isJsonValid(jsonSchema.toString(), objectValue.toString())) {
				throw new Exception("Schema Validation Failed. Invalid Person data");
			}
		}
		personKeyList = personService.post(person, version, baseURL);
		return new ResponseEntity<List<PersonKeys>>(personKeyList, HttpStatus.CREATED);
	}

}
