package io.konig.privacy.deidentification.service;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.model.PersonWithMetadata;
import io.konig.privacy.deidentification.repo.PersonRepository;


// TODO:  Consider eliminating PersonServiceImpl.  Shouldn't we make PersonRepository implement the PersonService interface?

@Service
public class PersonServiceImpl implements PersonService {

	@Autowired
	PersonRepository personRepository;

	@Override
	public PersonKeys postSensitivePII(PersonWithMetadata metaPerson) throws HttpClientErrorException, IOException,Exception{
		PersonKeys keys=personRepository.put(metaPerson);
		return keys;
	}

	@Override
	public boolean streamPersonalInformation(PersonKeys keys, PrintWriter writer) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean streamAnnotatedPersonalInformation(PersonKeys keys, PrintWriter writer) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public JsonNode getAnnotatedSensitivePII (String version, String pseudonym) throws DataAccessException, JsonProcessingException, IOException {
		if(!personRepository.annotatedPIIExists(version,pseudonym)){
			throw new NotFoundException("Person data not exists");
		}
		JsonNode jsonNode= personRepository.getAnnotatedSensitivePII(version,pseudonym); 
		return jsonNode;
	}

}
