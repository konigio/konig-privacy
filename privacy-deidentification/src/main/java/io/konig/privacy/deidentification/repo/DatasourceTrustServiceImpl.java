package io.konig.privacy.deidentification.repo;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import net.spy.memcached.MemcachedClient;

@Repository
public class DatasourceTrustServiceImpl implements DatasourceTrustService { 
	private static Logger logger = LoggerFactory.getLogger(DatasourceTrustServiceImpl.class);

	private JdbcTemplate template;
	
	private MemcachedClient cache;
	
	private Environment env;
	
	public DatasourceTrustServiceImpl(JdbcTemplate template, MemcachedClient cache, Environment env) {
		this.template = template;
		this.cache = cache;
		this.env = env;
	}

	private Map<String,Double> datasourceMap =new HashMap<String,Double>();
    
	public double getTrustLevel(String datasourceId) {

		Double trustLevel = datasourceMap.get(datasourceId);
		try{
			if (trustLevel == null) {		
				trustLevel = (Double) cache.get(datasourceId);
				if (trustLevel == null) {				
					String query = "SELECT TRUST_LEVEL FROM DE_IDENTIFICATION.DATASOURCE where ID=?";
					trustLevel = template.queryForObject(query, double.class, datasourceId);
					logger.debug("trustLevel in impl class: {}", trustLevel);	
					cache.set(datasourceId, Integer.parseInt(env.getProperty("aws.memcache.expirytime")), trustLevel);
				}
				datasourceMap.put(datasourceId, trustLevel);
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
    	

		return trustLevel;
    }

}