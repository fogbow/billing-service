package cloud.fogbow.fs.api.parameters;

import java.util.Map;

public class User {
	private String userId;
	private String provider;
	private String financePluginName;
	private Map<String, String> financeOptions;
	
	public User() {
		
	}
	
	public User(String userId, String provider, String financePluginName, Map<String, String> financeOptions) {
		this.userId = userId;
		this.provider = provider;
		this.financePluginName = financePluginName;
		this.financeOptions = financeOptions;
	}

	public String getUserId() {
		return userId;
	}
	
	public String getProvider() {
		return provider;
	}
	
	public String getFinancePluginName() {
		return financePluginName;
	}
	
	public Map<String, String> getFinanceOptions() {
		return financeOptions;
	}
}
