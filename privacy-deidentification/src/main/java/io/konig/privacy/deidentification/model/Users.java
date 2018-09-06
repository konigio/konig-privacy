package io.konig.privacy.deidentification.model;

public class Users {

	private String userName;
	private String password;
	private String permissions;
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPermissions() {
		return permissions;
	}
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
	public String toString() {
		return userName+","+permissions;
	}
}
