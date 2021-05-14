package cloud.fogbow.fs.core.datastore;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserId;

@Component
public class DatabaseManager {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private FinancePlanRepository financePlanRepository;
	
	public DatabaseManager() {
	}

    public void saveUser(FinanceUser user) {
        userRepository.save(user);
    }
	
	public void removeUser(String userId, String provider) throws InvalidParameterException {
	    FinanceUser user = userRepository.findByUserId(new UserId(userId, provider));
		userRepository.delete(user);
	}

	public List<FinanceUser> getRegisteredUsers() {
	    return userRepository.findAll();
	}

	public void saveFinancePlan(FinancePlan financePlan) {
	    financePlanRepository.save(financePlan);
	}

	public List<FinancePlan> getRegisteredFinancePlans() {
	    return financePlanRepository.findAll();
	}
	
	public void removeFinancePlan(String planName) {
	    financePlanRepository.delete(financePlanRepository.getOne(planName));
	}
}
