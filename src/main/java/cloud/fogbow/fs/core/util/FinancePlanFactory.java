package cloud.fogbow.fs.core.util;

import java.util.Map;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.FinancePlan;

public class FinancePlanFactory {
    
    public FinancePlan createFinancePlan(String planName, Map<String, String> planInfo) 
            throws InvalidParameterException {
        return new FinancePlan(planName, planInfo); 
    }
    
    public FinancePlan createFinancePlan(String planName, String planInfoFilePath) 
            throws InvalidParameterException {
        return new FinancePlan(planName, planInfoFilePath); 
    }
}
