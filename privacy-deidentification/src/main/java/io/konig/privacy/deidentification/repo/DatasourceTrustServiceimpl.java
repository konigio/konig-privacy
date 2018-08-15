package io.konig.privacy.deidentification.repo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import net.spy.memcached.MemcachedClient;

@Repository
public class DatasourceTrustServiceimpl implements DatasourceTrustService { 

	@Autowired
	JdbcTemplate template;
	
	@Autowired
	MemcachedClient cache;
	
	@Autowired
	private Environment env;
	
	private Map<String,Double> datasourceMap =new HashMap<String,Double>();
    
	public double getTrustLevel(String datasourceId) {
		double trustLevel = 0.00D;		
		try{
			if (datasourceMap.get(datasourceId) == null) {			
				if (cache.get(datasourceId) == null) {				
					String query = "SELECT TRUST_LEVEL FROM DE_IDENTIFICATION.DATASOURCE where ID=?";
					trustLevel = template.queryForObject(query, double.class, datasourceId);
					System.out.println("trustLevel in impl class"+trustLevel);					
					datasourceMap.put(datasourceId, trustLevel);
					cache.set(datasourceId, Integer.parseInt(env.getProperty("aws.memcache.expirytime")), trustLevel);
				}
				else{
					trustLevel=(double) cache.get(datasourceId);
				}
			}
			else{
				trustLevel= datasourceMap.get(datasourceId);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
    	

		return trustLevel;
    }

}