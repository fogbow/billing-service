package cloud.fogbow.fs.core.plugins.plan.postpaid;

import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.StoppableRunner;
import cloud.fogbow.fs.core.util.TimeUtils;
import cloud.fogbow.fs.core.util.accounting.Record;

public class PaymentRunner extends StoppableRunner {
    private static Logger LOGGER = Logger.getLogger(PaymentRunner.class);
    /**
     * The key to use to indicate the amount of time to 
     * wait between consecutive billing processes.
     */
    public static final String USER_BILLING_INTERVAL = "billing_interval";
    private InMemoryUsersHolder userHolder;
    private AccountingServiceClient accountingServiceClient;
    private InvoiceManager invoiceGenerator;
    private TimeUtils timeUtils;
    private long billingInterval;
    private String planName;
    
    public PaymentRunner(String name, long invoiceWaitTime, long billingInterval, InMemoryUsersHolder userHolder,
            AccountingServiceClient accountingServiceClient, InvoiceManager invoiceManager) {
        this.planName = name;
        this.timeUtils = new TimeUtils();
        this.sleepTime = invoiceWaitTime;
        this.billingInterval = billingInterval;
        this.accountingServiceClient = accountingServiceClient;
        this.invoiceGenerator = invoiceManager;
        this.userHolder = userHolder;
    }

    public PaymentRunner(String name, long invoiceWaitTime, long billingInterval, InMemoryUsersHolder userHolder,
            AccountingServiceClient accountingServiceClient, InvoiceManager invoiceManager, 
            TimeUtils timeUtils) {
        this.planName = name;
        this.sleepTime = invoiceWaitTime;
        this.billingInterval = billingInterval;
        this.userHolder = userHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.invoiceGenerator = invoiceManager;
        this.timeUtils = timeUtils;
    }

    private long getUserLastBillingTime(FinanceUser user) {
        String lastBillingTimeProperty = user.getProperty(FinanceUser.USER_LAST_BILLING_TIME);
        return Long.valueOf(lastBillingTimeProperty);
    }

    @Override
    public void doRun() {
        MultiConsumerSynchronizedList<FinanceUser> registeredUsers = userHolder.
                getRegisteredUsersByPlan(this.planName);
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
            
            if (isBillingTime(billingTime, lastBillingTime, billingInterval)) {
                tryToGenerateInvoiceForUser(user, billingTime, lastBillingTime);
            }
        }
    }

    private void tryToGenerateInvoiceForUser(FinanceUser user, long billingTime, long lastBillingTime) {
        try {
            List<Record> records = acquireUsageData(user, billingTime, lastBillingTime);
            this.invoiceGenerator.generateInvoiceForUser(user.getId(), user.getProvider(),
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
