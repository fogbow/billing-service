package cloud.fogbow.fs.api.parameters;

import com.google.gson.Gson;

import cloud.fogbow.ras.core.models.RasOperation;

public class AuthorizableUser {
	private String userToken;
	private String operation;
	
	public AuthorizableUser() {
		
	}
	
	public AuthorizableUser(String userToken, String operation) {
		this.userToken = userToken;
		this.operation = operation;
	}
	
	public String getUserToken() {
		return userToken;
	}
	
	public RasOperation getOperation() {
		return new Gson().fromJson(operation, RasOperation.class);
	}
}
