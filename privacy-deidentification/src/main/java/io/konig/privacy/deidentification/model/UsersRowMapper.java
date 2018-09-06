package io.konig.privacy.deidentification.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.ResultSetExtractor;

public class UsersRowMapper implements ResultSetExtractor<List<Users>> {
	List<Users> users = new ArrayList<>();

	@Override
	public List<Users> extractData(ResultSet rs) throws SQLException {
		try {
			String pUserName = "";
			String tPermissions = "";
			Users user = null;
			while (rs.next()) {
				String userName = rs.getString("userName");
				String permissions = rs.getString("permissions");
				if (userName.equals(pUserName)) {
					tPermissions = permissions + "|" + user.getPermissions();
					user.setPermissions(tPermissions);
				} else {
					if (user != null) {
						users.add(user);
					}
					user = new Users();
					tPermissions = "";
					user.setPermissions(permissions);
				}
				user.setUserName(userName);
				pUserName = userName;
				if (rs.isLast()) {
					users.add(user);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return users;
	}
}
