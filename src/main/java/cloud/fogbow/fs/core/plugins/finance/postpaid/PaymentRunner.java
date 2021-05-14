package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.finance.StoppableRunner;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.accounting.Record;

public class PaymentRunner extends StoppableRunner {
	private static Logger LOGGER = Logger.getLogger(PaymentRunner.class);
    /**
     * The key to use to indicate the amount of time to 
     * wait between consecutive billing processes.
     */
    public static final String USER_BILLING_INTERVAL = "billing_interval";
    private InMemoryFinanceObjectsHolder objectHolder;
	private AccountingServiceClient accountingServiceClient;
	private PaymentManager paymentManager;
	private TimeUtils timeUtils;
	
	public PaymentRunner(long invoiceWaitTime, InMemoryFinanceObjectsHolder objectHolder,
            AccountingServiceClient accountingServiceClient, PaymentManager paymentManager) {
        this.timeUtils = new TimeUtils();
        this.sleepTime = invoiceWaitTime;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.objectHolder = objectHolder;
	}

    public PaymentRunner(long invoiceWaitTime, InMemoryFinanceObjectsHolder objectHolder,
            AccountingServiceClient accountingServiceClient, PaymentManager paymentManager, 
            TimeUtils timeUtils) {
        this.timeUtils = timeUtils;
        this.sleepTime = invoiceWaitTime;
        this.accountingServiceClient = accountingServiceClient;
        this.paymentManager = paymentManager;
        this.objectHolder = objectHolder;
    }

	private long getUserLastBillingTime(FinanceUser user) {
		String lastBillingTimeProperty = user.getProperty(FinanceUser.USER_LAST_BILLING_TIME);
		return Long.valueOf(lastBillingTimeProperty);
	}

	private long getUserBillingInterval(FinanceUser user) {
		return Long.valueOf(user.getProperty(USER_BILLING_INTERVAL));
	}

	@Override
	public void doRun() {
	    MultiConsumerSynchronizedList<FinanceUser> registeredUsers = objectHolder.
		        getRegisteredUsersByPaymentType(PostPaidFinancePlugin.PLUGIN_NAME);
	    Integer consumerId = registeredUsers.startIterating();
	    
	    try {
	        FinanceUser user = registeredUsers.getNext(consumerId);
	        
	        while (user != null) {
	            tryToRunPaymentForUser(user);
	            user = registeredUsers.getNext(consumerId);
	        }
	    } catch (ModifiedListException e) {
            LOGGER.debug(Messages.Log.USER_LIST_CHANGED_SKIPPING_INVOICE_GENERATION);
        } catch (InternalServerErrorException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_GENERATE_INVOICE, e.getMessage()));
        } finally {
	        registeredUsers.stopIterating(consumerId);
	    }

		checkIfMustStop();
	}

    private void tryToRunPaymentForUser(FinanceUser user) {
        synchronized(user) {
            long billingTime = this.timeUtils.getCurrentTimeMillis();
            long lastBillingTime = getUserLastBillingTime(user);
            long billingInterval = getUserBillingInterval(user);
            
            if (isBillingTime(billingTime, lastBillingTime, billingInterval)) {
                tryToGenerateInvoiceForUser(user, billingTime, lastBillingTime);
            }
        }
    }

    private void tryToGenerateInvoiceForUser(FinanceUser user, long billingTime, long lastBillingTime) {
        try {
            List<Record> records = acquireUsageData(user, billingTime, lastBillingTime);
            this.paymentManager.startPaymentProcess(user.getId(), user.getProvider(),
                    lastBillingTime, billingTime, records);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.FAILED_TO_GENERATE_INVOICE_FOR_USER, user.getId(), e.getMessage()));
        }
    }

    private List<Record> acquireUsageData(FinanceUser user, long billingTime, long lastBillingTime) throws FogbowException {
        return this.accountingServiceClient.getUserRecords(user.getId(),
                user.getProvider(), lastBillingTime, billingTime);
    }

	private boolean isBillingTime(long billingTime, long lastBillingTime, long billingInterval) {
		return billingTime - lastBillingTime >= billingInterval;
	}
}