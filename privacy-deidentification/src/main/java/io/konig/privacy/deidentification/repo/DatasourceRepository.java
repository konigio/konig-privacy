package io.konig.privacy.deidentification.repo;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.konig.privacy.deidentification.model.Datasource;
import io.konig.privacy.deidentification.model.DatasourceResultSetExtractor;

@Repository
@Transactional
public class DatasourceRepository {

	@Autowired
    JdbcTemplate template;
	
    public Datasource getByUuid(String uId){
        String query1 = "select UID,ID,NAME,DE_IDENTIFICATION.DATASOURCE_NAME.LANGUAGE,DE_IDENTIFICATION.DATASOURCE_NAME.VALUE, DE_IDENTIFICATION.DATASOURCE_DESCRIPTION.Language AS DESCRIPTION_LANGUAGE ,DE_IDENTIFICATION.DATASOURCE_DESCRIPTION.VALUE as DESCRIPTION_VALUE, TRUST_LEVEL from DE_IDENTIFICATION.DATASOURCE INNER JOIN DE_IDENTIFICATION.DATASOURCE_NAME ON DE_IDENTIFICATION.DATASOURCE.UID=DE_IDENTIFICATION.DATASOURCE_NAME.DATASOURCE_UID  INNER JOIN DE_IDENTIFICATION.DATASOURCE_DESCRIPTION ON DE_IDENTIFICATION.DATASOURCE.UID=DE_IDENTIFICATION.DATASOURCE_DESCRIPTION.DATASOURCE_UID  and DE_IDENTIFICATION.DATASOURCE_DESCRIPTION.LANGUAGE=DE_IDENTIFICATION.DATASOURCE_NAME.LANGUAGE and UID=?";
        DatasourceResultSetExtractor extractor = new DatasourceResultSetExtractor();
        Datasource datasource = template.query(query1, extractor, uId);              
        return datasource;
    }
    public void updateDatasourceByUuid(String uuid,Datasource datasource){
    	datasource.setUuid(uuid);
    	deleteDatasourcebyUid(uuid);
    	insertDataSource(datasource);
    }
    
    public boolean datasourceExists(String Uid) {
    	String query="SELECT COUNT(*) FROM DE_IDENTIFICATION.DATASOURCE WHERE UID=?";
    	int count = template.queryForObject(query, Integer.class, Uid);
		if(count == 0) {
			return false;
		} else {
			return true;
		}
    }
    public void deleteDatasourcebyUid(String Uid){
    	
    	String query2 = "DELETE FROM DE_IDENTIFICATION.DATASOURCE_DESCRIPTION WHERE DATASOURCE_UID=?";
    	template.update(query2, Uid);
    	
    	String query1 = "DELETE FROM DE_IDENTIFICATION.DATASOURCE_NAME WHERE DATASOURCE_UID=?";
    	template.update(query1, Uid);
    	
    	String query = "DELETE FROM DE_IDENTIFICATION.DATASOURCE WHERE UID=?";
    	template.update(query, Uid);
    	
    }
    
    public String registerDatasource(Datasource datasource){
    	String uuid=null;
    	uuid=StringUtils.capitalize(randomString(30));
    	datasource.setUuid(uuid);
    	insertDataSource(datasource);    	
    	return uuid;
    }
    
    public  String randomString(int length){
		String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789absdefghijklmnopqrstuvwxyz";
		Random random = new Random();
	    StringBuilder b = new StringBuilder();
	    b.append(base.charAt(random.nextInt(26)));
	    for(int i = 0; i < length; i++){
	      b.append(base.charAt(random.nextInt(base.length()-1)));
	    }
	    return b.toString();
	  }
    
    public void insertDataSource (Datasource datasource){
    	String query="INSERT INTO  DE_IDENTIFICATION.DATASOURCE (UID, ID, TRUST_LEVEL) VALUES (?,?,?)";
    	template.update(query, datasource.getUuid(),datasource.getId(),datasource.getTrustLevel());
    	
    	String query_1= "INSERT INTO DE_IDENTIFICATION.DATASOURCE_NAME (DATASOURCE_UID,VALUE, LANGUAGE) VALUES (?,?,?)";    	
    	for(int i=0;i<datasource.getName().size();i++){
    		template.update(query_1,datasource.getUuid(),datasource.getName().get(i).getValue(),datasource.getName().get(i).getLanguage());
    	}
    	
    	String query_2= "INSERT INTO DE_IDENTIFICATION.DATASOURCE_DESCRIPTION (DATASOURCE_UID,VALUE, LANGUAGE) VALUES (?,?,?)";
    	for(int i=0;i<datasource.getDescription().size();i++){
    		template.update(query_2,datasource.getUuid(),datasource.getDescription().get(i).getValue(),datasource.getDescription().get(i).getLanguage());
    	}
    }
}
