package cloud.fogbow.fs.core.plugins.plan.prepaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.fs.core.util.StoppableRunner;

public class StopServiceRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
	
	private String planName;
	private InMemoryUsersHolder usersHolder;
	private CreditsManager paymentManager;
	private RasClient rasClient;
	
    public StopServiceRunner(String planName, long stopServiceWaitTime, InMemoryUsersHolder usersHolder, 
            CreditsManager paymentManager, RasClient rasClient) {
        this.planName = planName;
        this.sleepTime = stopServiceWaitTime;
        this.usersHolder = usersHolder;
        this.paymentManager = paymentManager;
        this.rasClient = rasClient;
    }

	@Override
	public void doRun() {
		// This runner depends on PrePaidPlanPlugin. Maybe we should 
		// pass the plugin name as an argument to the constructor, so we 
		// can reuse this class.
	    
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = 
	            this.usersHolder.getRegisteredUsersByPlan(this.planName);
	    Integer consumerId = registeredUsers.startIterating();
	    
	    try {
	        FinanceUser user = registeredUsers.getNext(consumerId);
	        
	        while (user != null) {
	            tryToCheckUserState(user);
	            user = registeredUsers.getNext(consumerId);
	        }
	    } catch (ModifiedListException e) {
	        LOGGER.debug(Messages.Log.USER_LIST_CHANGED_SKIPPING_USER_PAYMENT_STATE_CHECK);
        } catch (InternalServerErrorException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_MANAGE_RESOURCES, e.getMessage()));
        } finally {
	        registeredUsers.stopIterating(consumerId);
	    }
		
		checkIfMustStop();
	}

    private void tryToCheckUserState(FinanceUser user) throws InternalServerErrorException {
        synchronized (user) {
            try {
                boolean paid = this.paymentManager.hasPaid(user.getId(), user.getProvider());
                boolean stoppedResources = user.stoppedResources();

                if (!paid && !stoppedResources) {
                    tryToPauseResources(user);
                }

                if (paid && stoppedResources) {
                    tryToResumeResources(user);
                }
            } catch (InvalidParameterException e) {
                LOGGER.error(String.format(Messages.Log.UNABLE_TO_FIND_USER, user.getId(), user.getProvider()));
            }
        }
    }

    private void tryToPauseResources(FinanceUser user) {
        try {
            this.rasClient.pauseResourcesByUser(user.getId());
            user.setStoppedResources(true);
            this.usersHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER, user.getId(),
                    e.getMessage()));
        }
    }
    
    private void tryToResumeResources(FinanceUser user) {
        try {
            this.rasClient.resumeResourcesByUser(user.getId());
            user.setStoppedResources(false);
            this.usersHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES_FOR_USER, user.getId(),
                    e.getMessage()));
        }
    }
}