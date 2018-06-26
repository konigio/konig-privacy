package io.konig.privacy.deidentification.service;

import io.konig.privacy.deidentification.model.Datasource;

public interface DatasourceService {

	/**
	 * Store information about the supplied datasource, completely overwriting the existing information
	 * about that datasource.  If a UUID already exists for the datasource, then that UUID value will be preserved.
	 * Otherwise, a UUID will be generated.
	 * 
	 * @param datasource The Datasource record that is to be stored.
	 * @return The UUID for the Datasource.
	 * @throws DataAccessException If an error occurred while storing the Datasource record.
	 */
	public String put(Datasource datasource) throws DataAccessException, Exception;
	
	/**
	 * Get a Datasource record based on the UUID for the datasource.
	 * 
	 * @param uuid  The UUID for the Datasource whose record is to be returned.
	 * @return A record of the Datasource with the specified UUID, or null if no such record is found.
	 * @throws DataAccessException  If an error occurred while fetching the record.
	 */
	public Datasource getByUuid(String uuid) throws DataAccessException;
	
	/**
	 * Update the Datasource based on the UUId of the datasource.
	 * @param uid
	 * @param datasource
	 * @throws DataAccessException
	 */
	public void updateDatasourceByUid (String uid,Datasource datasource) throws DataAccessException;
	
	/**
	 * Delete Datasource record based on UUID of the datasource.
	 * @param Uid
	 * @throws DataAccessException
	 */
	public void deleteDatasourceByUid (String Uid) throws DataAccessException;
}
