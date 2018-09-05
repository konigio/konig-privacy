package io.konig.privacy.deidentification.service;

import java.util.List;

import io.konig.privacy.deidentification.model.Users;

public interface UserService {
	
	public void uploadUserAccounts(List<Users> userList)throws Exception;
	
	public List<Users> getUserAccounts() throws DataAccessException;
	
	public void deleteUserAccounts(List<Users> userList)throws DataAccessException;


}
