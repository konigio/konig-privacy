package io.konig.privacy.deidentification.rest;

import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import io.konig.privacy.deidentification.model.Datasource;
import io.konig.privacy.deidentification.service.DataAccessException;
import io.konig.privacy.deidentification.service.DatasourceService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping(value={"/api"}) 
public class DatasourceController {
	
	@Autowired
	DatasourceService dataSourceService;
	
	@RequestMapping(value="/datasource/{uId}" , method = RequestMethod.GET)
    @ResponseBody
    public Datasource getDatasource(@PathVariable("uId") String uId) throws DataAccessException{	
		Datasource datasource=null;
       	datasource= dataSourceService.getByUuid(uId);
       return datasource;
    }
	
	@RequestMapping(value="/datasource/{uId}" , method = RequestMethod.PUT)
	public void updateDatasource(@PathVariable("uId") String uId, @RequestBody String strBody) throws DataAccessException{
		Gson gson = new Gson();
		Datasource datasource=gson.fromJson(strBody, Datasource.class);
		dataSourceService.updateDatasourceByUid(uId,datasource);
	}
	
	@RequestMapping(value="/datasource/{uId}" , method = RequestMethod.DELETE)
	public void deleteDatasource(@PathVariable("uId") String uId) throws DataAccessException{
		dataSourceService.deleteDatasourceByUid(uId);
	}
	
	@RequestMapping(value="/datasource" , method = RequestMethod.POST)
	public ResponseEntity<?> registerDatasource(@RequestBody String strBody) throws DataAccessException, Exception{
		Gson gson = new Gson();
		Datasource datasource=gson.fromJson(strBody, Datasource.class);
		String Uuid=dataSourceService.put(datasource);
		HttpHeaders responseHeaders = new HttpHeaders();
		String myURL= "/datasource/"+Uuid;
		URI location= URI.create(myURL);
		responseHeaders.setLocation(location);
	    HashMap<String, Object> hmap = new HashMap<String, Object>();
	    hmap.put("id", datasource.getId());
	    hmap.put("uid", Uuid);
	    return new ResponseEntity<HashMap<String, Object>>(hmap,responseHeaders, HttpStatus.CREATED);
	}
	
	@RequestMapping(value="/health")
	 public @ResponseBody String health() {
		System.out.println("In Health");
		return "200 OK";
	}

}
