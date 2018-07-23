package io.konig.privacy.deidentification.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class PersonRowMapper implements RowMapper{
	PersonData personData=new PersonData();
	@Override
	public Object mapRow(ResultSet row, int rowNum) throws SQLException {
		personData.setPerson(row.getString("PERSON_DATA"));
		personData.setPseudonym(row.getString("PSEUDONYM"));
		return personData;
	}
}
