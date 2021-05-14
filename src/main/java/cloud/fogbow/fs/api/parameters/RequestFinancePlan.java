package cloud.fogbow.fs.api.parameters;

import java.util.Map;

public class RequestFinancePlan {
	private String planName;
	private Map<String, String> planInfo;
	
	public RequestFinancePlan() {
		
	}
	
	public RequestFinancePlan(String planName, Map<String, String> planInfo) {
		this.planName = planName;
		this.planInfo = planInfo;
	}

	public String getPlanName() {
		return planName;
	}
	
	public Map<String, String> getPlanInfo() {
		return planInfo;
	}
}
