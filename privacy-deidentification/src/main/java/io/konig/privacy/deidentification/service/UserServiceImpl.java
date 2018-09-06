package io.konig.privacy.deidentification.service;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.konig.privacy.deidentification.model.Users;
import io.konig.privacy.deidentification.repo.UserRepository;
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	UserRepository userRepository;
	
	@Override
	public void uploadUserAccounts(List<Users> userList) throws Exception {
		
		userRepository.uploadUserAccounts(userList);
		
	}
	
	@Override
	public List<Users> getUserAccounts() throws DataAccessException {
		return userRepository.getUserAccounts();
	}

	@Override
	public void deleteUserAccounts(List<Users> userList) throws DataAccessException {
		userRepository.deleteUserAccounts(userList);
		
	}

}
