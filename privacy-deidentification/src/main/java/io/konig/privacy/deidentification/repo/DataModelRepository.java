package io.konig.privacy.deidentification.repo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.konig.privacy.deidentification.service.DataAccessException;


@Repository
@Transactional
public class DataModelRepository {
	
	@Autowired
    JdbcTemplate template;
	
	public JsonNode getSchemaByVersion(String version) throws JsonProcessingException, IOException{
		String query="SELECT JSONSCHEMA from DE_IDENTIFICATION.DATA_MODEL where version=?";
		String jsonString=template.queryForObject(query, new Object[] { version }, String.class);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(jsonString);
		return jsonNode;
	}
	
	public String getLatestDataModel() throws DataAccessException {
		String query="SELECT concat('V',max(Version)) from DE_IDENTIFICATION.DATA_MODEL";
		String latestVersion=template.queryForObject(query, String.class);	
		return latestVersion;
	}
	
    public boolean dataModelExists(String version) {
    	String query="SELECT COUNT(*) FROM DE_IDENTIFICATION.DATA_MODEL WHERE VERSION=?";
    	int count = template.queryForObject(query, Integer.class, version);
		if(count == 0) {
			return false;
		} else {
			return true;
		}
    }
    
    public String put(String jsonStr){    	
    	GeneratedKeyHolder holder = new GeneratedKeyHolder();
    	template.update(new PreparedStatementCreator() {
    	    @Override
    	    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
    	        PreparedStatement statement = con.prepareStatement("INSERT INTO DE_IDENTIFICATION.DATA_MODEL(JSONSCHEMA) VALUES (?) ", Statement.RETURN_GENERATED_KEYS);
    	        statement.setNString(1, jsonStr);
    	        return statement;
    	    }
    	}, holder);

    	int primaryKey = holder.getKey().intValue();
    	String version= "V"+Integer.toString(primaryKey);
    	return version;
    }

}