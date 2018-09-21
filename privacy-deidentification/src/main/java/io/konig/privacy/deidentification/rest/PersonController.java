package io.konig.privacy.deidentification.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;

import io.konig.privacy.deidentification.model.DataModel;
import io.konig.privacy.deidentification.model.Metadata;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonWithMetadata;
import io.konig.privacy.deidentification.model.Provenance;
import io.konig.privacy.deidentification.repo.DatasourceTrustService;
import io.konig.privacy.deidentification.repo.DatasourceTrustServiceImpl;
import io.konig.privacy.deidentification.service.DataModelService;
import io.konig.privacy.deidentification.service.PersonSchemaService;
import io.konig.privacy.deidentification.service.PersonService;
import io.konig.privacy.deidentification.utils.ValidationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.spy.memcached.MemcachedClient;

@RestController
@RequestMapping(value = { "/api" })
@Api(value = "PersonService")
public class PersonController {

	@Autowired
	PersonService personService;

	@Autowired
	DataModelService dataModelService;

	@Autowired
	JdbcTemplate template;

	@Autowired
	MemcachedClient cache;
	
	@Autowired
	PersonSchemaService personSchemaService;

	@Autowired
	private Environment env;

	@ApiOperation(value = "API to get pseudonyms for a batch of people", response = PersonKeys.class)
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Created"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(value = "/privacy/{version}/person", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<List<PersonKeys>> postSensitivePII(@PathVariable("version") String version,
			@RequestBody String strBody) throws Exception {
		List<PersonKeys> personKeyList = new ArrayList<PersonKeys>();
		ObjectMapper mapper = new ObjectMapper();
		ObjectMapper jsonSchemaMapper = new ObjectMapper();
		DatasourceTrustService.instance.set(new DatasourceTrustServiceImpl(template, cache, env));
		
		try {

			JsonNode actualObj = mapper.readTree(strBody);
			String baseURL = env.getProperty("baseURL");
			PersonKeys keys = new PersonKeys();
			ArrayNode personArray = (ArrayNode) actualObj.findValue("data");
			String tempVersion = version.substring(1);
			
			// TODO: We might want to consider refactoring the code to extract the datamodel version from the
			//       request schema (since the data model schema is embedded within).
			
			String requestSchema = personSchemaService.pseudonymsRequest(tempVersion);
			
			System.out.println("requestSchema"+requestSchema);
			
			JsonSchema schemaNode = ValidationUtils.getSchemaNode(requestSchema);
			
			ProcessingReport report = schemaNode.validate(actualObj);
			
			if(!report.isSuccess()){
				Iterator<ProcessingMessage> sequence = report.iterator();
				String error=null;
		        while (sequence.hasNext()) {
		        	ProcessingMessage msg = sequence.next();
		        	error=msg.getMessage();
		        	System.out.println(msg.getMessage());
		        }
		        throw new Exception("Schema Validation Failed. Invalid Person data due to "+error);
			}
						

			Metadata metaData = new Metadata();
			DataModel dataModel = new DataModel();
			dataModel.setVersion(version);
			JsonNode jsonNodeSchema=jsonSchemaMapper.readTree(requestSchema);
			dataModel.setJsonSchema(jsonNodeSchema);
			metaData.setDataModel(dataModel);
			Provenance provenance = new Provenance();
			String dataSourceId = actualObj.findValue("datasource").textValue();
			provenance.setReceivedFrom(dataSourceId);

			GregorianCalendar gregoriancalendar = new GregorianCalendar();

			provenance.setReceivedAtTime(gregoriancalendar);
			metaData.setProvenance(provenance);
			
			
			// This solution iterates through each Person in the batch and makes a separate call to the 
			// database via personService.
			
			// This is necessary because PersonService only has methods for operating on one person at a time.
			
			// TODO:  Consider refactoring the solution so that the PersonService can operate on a batch of values with a single
			// call to the database (or a small number of calls).  
			
			// We should perform this refactoring only if performance tests demand it.
			

			PersonWithMetadata personWithMetaData = new PersonWithMetadata();
			
			for (int i = 0; i < personArray.size(); i++) {
				ObjectNode personObjectNode = (ObjectNode) personArray.get(i);
			
				personWithMetaData.setPerson(personObjectNode);
				personWithMetaData.setMetadata(metaData);
				keys = personService.postSensitivePII(personWithMetaData);
				keys.setId(baseURL + keys.getPseudonym());
				personKeyList.add(keys);
			}
			return new ResponseEntity<List<PersonKeys>>(personKeyList, HttpStatus.CREATED);
		} finally {
			DatasourceTrustService.instance.remove();
		}
	}

	@RequestMapping(value = "/privacy/{version}/person/{pseudonym}/.annotated", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<JsonNode> getAnnotatedSensitivePII(@PathVariable("version") String version,
			@PathVariable("pseudonym") String pseudonym) throws Exception, JsonProcessingException, IOException {
		JsonNode jsonNode = personService.getAnnotatedSensitivePII(version, pseudonym);
		return new ResponseEntity<JsonNode>(jsonNode, HttpStatus.OK);
	}

}
