package io.konig.privacy.deidentification.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.konig.privacy.deidentification.model.Identity;
import io.konig.privacy.deidentification.model.Person;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.service.DataModelService;
import io.konig.privacy.deidentification.service.PersonService;
import io.konig.privacy.deidentification.utils.ValidationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping(value = { "/api" })
@Api(value="PersonService")
public class PersonController {

	@Autowired
	PersonService personService;

	@Autowired
	DataModelService dataModelService;

	@Autowired
	private Environment env;

	@ApiOperation(value = "API to get pseudonyms for a batch of people",response=PersonKeys.class)
	@ApiResponses(value = {
	            @ApiResponse(code = 201, message = "Created"),
	            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),	            
	            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	    }
	    )
	@RequestMapping(value = "/privacy/{version}/person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<List<PersonKeys>> postSensitivePII(@PathVariable("version") String version, @RequestBody String strBody)
			throws Exception {
		List<PersonKeys> personKeyList = new ArrayList<PersonKeys>();
		List<PersonKeys> personList =new ArrayList<PersonKeys>();
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
		personKeyList = personService.post(person, version);
		for(int k=0;k<personKeyList.size();k++){
			PersonKeys personkeys=new PersonKeys();
			personkeys.setPseudonym(personKeyList.get(k).getPseudonym());
			PropertyUtils.setSimpleProperty(personkeys, "id", baseURL+personKeyList.get(k).getPseudonym());
			List<String> email =new ArrayList<String>();
			List<Identity> identityList = new ArrayList<Identity>();
			for(int j=0;j<personKeyList.get(k).getEmail().size();j++){
				email.add(personKeyList.get(k).getEmail().get(j));
			}
			for(int z=0;z<personKeyList.get(k).getIdentity().size();z++){
				Identity identity = new Identity(personKeyList.get(k).getIdentity().get(z).getIdentityProvider(),personKeyList.get(k).getIdentity().get(z).getIdentifier());
				identityList.add(identity);
			}
			personkeys.setEmail(email);
			personkeys.setIdentity(identityList);
			personList.add(personkeys);
		}
		return new ResponseEntity<List<PersonKeys>>(personList, HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/privacy/{version}/person/{pseudonym}/.annotated", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<JsonNode> getAnnotatedSensitivePII(@PathVariable("version") String version, @PathVariable("pseudonym") String pseudonym)
			throws Exception,JsonProcessingException, IOException {
		JsonNode jsonNode= personService.getAnnotatedSensitivePII(version,pseudonym);		
		return new ResponseEntity<JsonNode>(jsonNode, HttpStatus.OK); 
	}

}
