package cloud.fogbow.fs.core.plugins.finance.postpaid;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.RasClient;

public class StopServiceRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(StopServiceRunner.class);
	
	private InMemoryFinanceObjectsHolder objectHolder;
	private PaymentManager paymentManager;
	private RasClient rasClient;

    public StopServiceRunner(long stopServiceWaitTime, InMemoryFinanceObjectsHolder objectHolder, PaymentManager paymentManager,
            RasClient rasClient) {
        this.sleepTime = stopServiceWaitTime;
        this.objectHolder = objectHolder;
        this.paymentManager = paymentManager;
        this.rasClient = rasClient;
    }

	@Override
	public void doRun() {
		// This runner depends on PostPaidFinancePlugin. Maybe we should 
		// pass the plugin name as an argument to the constructor, so we 
		// can reuse this class.
	    
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = 
		        this.objectHolder.getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);
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
            this.objectHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_PAUSE_USER_RESOURCES_FOR_USER, user.getId(), 
                    e.getMessage()));
        }
    }
    
    private void tryToResumeResources(FinanceUser user) {
        try {
            this.rasClient.resumeResourcesByUser(user.getId());
            user.setStoppedResources(false);
            this.objectHolder.saveUser(user);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_RESUME_USER_RESOURCES_FOR_USER, user.getId(), 
                    e.getMessage()));
        }
    }
}