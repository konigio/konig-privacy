package io.konig.privacy.deidentification.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.konig.privacy.deidentification.repo.DataModelRepository;

@Service
public class DataModelServiceImpl implements DataModelService{
	
	@Autowired
	DataModelRepository dataModelRepository;
	
	public JsonNode getSchemaByVersion(String version) throws DataAccessException, JsonProcessingException, IOException {
		JsonNode jsonNode=null;
		if(!dataModelRepository.dataModelExists(version)){
			throw new NotFoundException("version=v"+version+" does not exist.");
		}
		jsonNode= dataModelRepository.getSchemaByVersion(version);
		return jsonNode;
		
	}
	
	public String getLatestDataModel() throws DataAccessException {
		String latestVersion= dataModelRepository.getLatestDataModel();
		return latestVersion;
	}
	
	public String put(String jsonStr) throws DataAccessException {
		String version= dataModelRepository.put(jsonStr);
		return version;
	}

}
