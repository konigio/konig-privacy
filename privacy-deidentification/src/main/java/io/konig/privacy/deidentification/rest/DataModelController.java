package io.konig.privacy.deidentification.rest;


import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

import io.konig.privacy.deidentification.service.DataAccessException;
import io.konig.privacy.deidentification.service.DataModelService;

@RestController
@RequestMapping(value={"/api"}) 
public class DataModelController {
	
	@Autowired
	DataModelService dataModelService;
	
	@RequestMapping(value="/schema/latest" , method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getLatestDataModel() throws DataAccessException, IOException{
		String latestVersion= dataModelService.getLatestDataModel();
		HttpHeaders responseHeaders = new HttpHeaders();
		String myURL= "/schema/"+latestVersion;
		URI location= URI.create(myURL);
		//response.sendRedirect(myURL);
		responseHeaders.add("Location", myURL);
		//responseHeaders.setLocation(location);
		return new ResponseEntity<>(responseHeaders,HttpStatus.MOVED_PERMANENTLY);
		//return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, myURL).build();
    }
	
	@RequestMapping(value="/schema/{version}" , method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> getDataModelByVersion(@PathVariable("version") String version) throws DataAccessException, JsonProcessingException, IOException{
		String tempVersion= version.substring(1);
		JsonNode jsonNode= dataModelService.getSchemaByVersion(tempVersion);
		return new ResponseEntity<JsonNode>(jsonNode,HttpStatus.OK);
    }
	
	@RequestMapping(value="/schema" , method = RequestMethod.POST)
	public ResponseEntity<?> insertDataModel(@RequestBody String strBody) throws DataAccessException, Exception{
		String version=null;
		version=dataModelService.put(strBody);
		HttpHeaders responseHeaders = new HttpHeaders();
		String myURL= "/schema/"+version;
		URI location= URI.create(myURL);
		responseHeaders.setLocation(location);	    
	    return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
	}
	

}