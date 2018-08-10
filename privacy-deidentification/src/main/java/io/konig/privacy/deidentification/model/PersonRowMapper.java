package io.konig.privacy.deidentification.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class PersonRowMapper implements RowMapper<PersonData>{
	PersonData personData=new PersonData();
	@Override
	public PersonData mapRow(ResultSet row, int rowNum) throws SQLException {
		personData.setPerson(row.getString("PERSON_DATA"));
		personData.setAnnotated_person(row.getString("ANNOTATED_PERSON_DATA"));
		personData.setPseudonym(row.getString("PSEUDONYM"));
		return personData;
	}
}
