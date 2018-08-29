package io.konig.privacy.deidentification.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.konig.privacy.deidentification.model.Users;

@Repository
@Transactional
public class UserRepository {
	
	@Autowired
	JdbcTemplate template;

	
	public void uploadDatasourceUsers(List<Users> userList){
		
		for(Users user : userList){
			
			String query ="INSERT INTO DE_IDENTIFICATION.USERS (USERNAME,SHA2PASSWORD,ENABLED) VALUES(?,?,?)";
			template.update(query,user.getUserName(),user.getPassword(),true);
			
			String query_1 = "INSERT INTO DE_IDENTIFICATION.USER_ROLES(USERNAME,ROLE) VALUES(?,?)";
			template.update(query_1,user.getUserName(),user.getPermissions());
		}
		
}

}
