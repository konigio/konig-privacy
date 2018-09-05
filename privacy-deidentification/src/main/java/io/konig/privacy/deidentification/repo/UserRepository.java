package io.konig.privacy.deidentification.repo;

import java.security.MessageDigest;
import java.util.List;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.konig.privacy.deidentification.model.Users;
import io.konig.privacy.deidentification.model.UsersRowMapper;
import io.konig.privacy.deidentification.rest.UserController;

@Repository
@Transactional
public class UserRepository {
	
	private static Logger logger = LoggerFactory.getLogger(UserRepository.class);

	@Autowired
	JdbcTemplate template;

	
	public void uploadUserAccounts(List<Users> userList) throws Exception{
		
		for(Users user : userList){
			
			String query ="INSERT INTO DE_IDENTIFICATION.USERS (USERNAME,PASSWORD,ENABLED) VALUES(?,?,?)";
			String sha2Password = user.getPassword();
			try {
				sha2Password = convertToSHA256String(sha2Password);
			} catch (ServletException e) {
				logger.info(e.getMessage());
				throw new Exception();
				}
			template.update(query,user.getUserName(),sha2Password,true);
			
			String query_1 = "INSERT INTO DE_IDENTIFICATION.USER_PERMISSIONS(USERNAME,PERMISSIONS) VALUES(?,?)";
			String[] permArr =user.getPermissions().split("\\|");
			if(permArr.length >0){
				for(int i = 0; i <permArr.length; i++){
					template.update(query_1,user.getUserName(),permArr[i]);

				}
				}
			}

}
	public List<Users> getUserAccounts() {
		String query = "SELECT DE_IDENTIFICATION.USERS.USERNAME as userName, DE_IDENTIFICATION.USER_PERMISSIONS.PERMISSIONS as permissions "
				+ "FROM DE_IDENTIFICATION.USERS  " 
				+ "INNER JOIN  DE_IDENTIFICATION.USER_PERMISSIONS "
				+ "ON DE_IDENTIFICATION.USERS.USERNAME = DE_IDENTIFICATION.USER_PERMISSIONS.USERNAME "
				+ "ORDER BY DE_IDENTIFICATION.USERS.USERNAME;";
		List<Users> users = template.query(query, new UsersRowMapper());
		return users;
	}


	public void deleteUserAccounts(List<Users> userList){
		
		for(Users user : userList){
			
			String query = "DELETE FROM  DE_IDENTIFICATION.USER_PERMISSIONS WHERE USERNAME = ?";
			template.update(query,user.getUserName());
			
			String query_1 ="DELETE FROM DE_IDENTIFICATION.USERS WHERE USERNAME = ?";
			template.update(query_1,user.getUserName());
		}
		
}
	
	private static String convertToSHA256String(String value) throws ServletException {
		try {			
			MessageDigest  md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = md.digest(value.getBytes());
			StringBuffer stringBuffer = new StringBuffer();
	        for (int i = 0; i < bytes.length; i++) {
	            stringBuffer.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16)
	                    .substring(1));
	        }
	        return stringBuffer.toString();
		}catch(Throwable e) {
			logger.info(e.getMessage());
			throw new ServletException(e);
		}
	}
	
}
