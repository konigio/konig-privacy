package io.konig.privacy.deidentification.service;

import java.util.List;

import io.konig.privacy.deidentification.model.Users;

public interface UserService {
	
	public  void uploadDatasourceUsers(List<Users> userList)throws DataAccessException;

}
