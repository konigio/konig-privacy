package io.konig.privacy.deidentification.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import io.konig.privacy.deidentification.model.Person;
import io.konig.privacy.deidentification.model.PersonKeys;
import io.konig.privacy.deidentification.repo.PersonRepository;

@Service
public class PersonServiceImpl implements PersonService {

	@Autowired
	PersonRepository personRepository;

	@Override
	public List<PersonKeys> post(Person person, String version, String baseURL)
			throws ProcessingException, IOException {
		// TODO Auto-generated method stub
		List<PersonKeys> personKeyList = new ArrayList<PersonKeys>();
		personKeyList = personRepository.put(person, version, baseURL);
		return personKeyList;
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

}
