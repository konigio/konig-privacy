/**
 * 
 */
package io.konig.privacy.deidentification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.konig.privacy.deidentification.model.Datasource;
import io.konig.privacy.deidentification.repo.DatasourceRepository;


@Service
public class DatasourceServiceImpl implements DatasourceService{
	
	@Autowired
	DatasourceRepository datasourceRepository;
	
	/**
	 * Get a Datasource record based on the UUID for the datasource.
	 */
	public Datasource getByUuid(String uuid) throws DataAccessException{
		if(!datasourceRepository.datasourceExists(uuid)){
			throw new NotFoundException("UID="+uuid+" does not exist.");
		}
		return datasourceRepository.getByUuid(uuid);
		
	}
	/**
	 * Store information about the supplied datasource
	 */
	public String put(Datasource datasource) throws DataAccessException, Exception{
		String uuid=null;
		if(datasource.getName()==null || datasource.getName().size()<1){
			throw new Exception("There should be atleast one Name list defined");
		}				
		uuid=datasourceRepository.registerDatasource(datasource);
		return uuid;
	}
	
	/**
	 * Update the Datasource based on the UUId of the datasource.
	 */
	public void updateDatasourceByUid(String uid,Datasource datasource) throws DataAccessException{
		if(!datasourceRepository.datasourceExists(uid)){
			throw new NotFoundException("UID="+uid+" does not exist.");
		}
		datasourceRepository.updateDatasourceByUuid(uid,datasource);
	}
	
	/**
	 * Delete Datasource record based on UUID of the datasource.
	 */
	public void deleteDatasourceByUid(String uid) throws DataAccessException{
		datasourceRepository.deleteDatasourcebyUid(uid);
	}
	
	
		
}
