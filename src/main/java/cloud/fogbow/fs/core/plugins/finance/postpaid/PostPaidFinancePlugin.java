package cloud.fogbow.fs.core.plugins.finance.postpaid;

import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.PaymentManagerInstantiator;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class PostPaidFinancePlugin implements FinancePlugin {
	/**
	 * A textual reference to this plugin. 
	 * Normally used as a user property to set this class
	 * as the one to be used to manage the user.
	 */
	public static final String PLUGIN_NAME = "postpaid";
	/**
	 * The key to use in the configuration property which 
	 * indicates the {@link cloud.fogbow.fs.core.plugins.PaymentManager}
	 * to use.
	 * Required in the configuration file. 
	 */
	public static final String POST_PAID_PAYMENT_MANAGER = "post_paid_payment_manager";
	/**
	 * The key to use in the configuration property which
	 * indicates the {@link cloud.fogbow.fs.core.models.FinancePlan}
	 * to use.
	 * Required in the configuration file.
	 */
	public static final String POST_PAID_DEFAULT_FINANCE_PLAN = "post_paid_default_finance_plan";
	/**
	 * The key to use in the configuration property which
	 * indicates the delay between invoice generation attempts.
	 * Normally the PaymentRunner checks if the user configuration
	 * allows the invoice generation at each attempt.
	 */
	public static final String INVOICE_WAIT_TIME = "invoice_wait_time";

	private Thread paymentThread;
	private Thread stopServiceThread;
	private PaymentManager paymentManager;
	private AccountingServiceClient accountingServiceClient; 
	private RasClient rasClient;
	private PaymentRunner paymentRunner;
	private StopServiceRunner stopServiceRunner;
	private long invoiceWaitTime;
	private boolean threadsAreRunning;
	private InMemoryFinanceObjectsHolder objectHolder;

    public PostPaidFinancePlugin(InMemoryFinanceObjectsHolder objectHolder) throws ConfigurationErrorException {
        this(objectHolder, new AccountingServiceClient(), new RasClient(),
                PaymentManagerInstantiator.getPaymentManager(
                        PropertiesHolder.getInstance().getProperty(POST_PAID_PAYMENT_MANAGER), objectHolder,
                        PropertiesHolder.getInstance().getProperty(POST_PAID_DEFAULT_FINANCE_PLAN)),
                Long.valueOf(PropertiesHolder.getInstance().getProperty(INVOICE_WAIT_TIME)));
    }
	
	public PostPaidFinancePlugin(InMemoryFinanceObjectsHolder objectHolder, AccountingServiceClient accountingServiceClient,
            RasClient rasClient, PaymentManager paymentManager, long invoiceWaitTime) {
	    this.objectHolder = objectHolder;
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.paymentManager = paymentManager;
        this.invoiceWaitTime = invoiceWaitTime;
        this.threadsAreRunning = false;
    }

    @Override
	public String getName() {
		return PLUGIN_NAME;
	}
	
	@Override
	public void startThreads() {
		if (!this.threadsAreRunning) {
			this.paymentRunner = new PaymentRunner(invoiceWaitTime, objectHolder, accountingServiceClient, paymentManager);
			this.paymentThread = new Thread(paymentRunner);
			
			this.stopServiceRunner = new StopServiceRunner(invoiceWaitTime, objectHolder, paymentManager, rasClient);
			this.stopServiceThread = new Thread(stopServiceRunner);
			
			this.paymentThread.start();
			this.stopServiceThread.start();
			
			while (!this.paymentRunner.isActive());
			while (!this.stopServiceRunner.isActive());
			
			this.threadsAreRunning = true;
		}
	}

	@Override
	public void stopThreads() {
		if (this.threadsAreRunning) {
			this.paymentRunner.stop();
			this.stopServiceRunner.stop();
			
			this.threadsAreRunning = false;
		}
	}

	@Override
	public boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException {
		if (operation.getOperationType().equals(Operation.CREATE)) {
			return this.paymentManager.hasPaid(user.getId(), user.getIdentityProviderId());
		}
		
		return true;
	}

	@Override
	public boolean managesUser(String userId, String provider) throws InternalServerErrorException, InvalidParameterException {
        FinanceUser user = this.objectHolder.getUserById(userId, provider);
        
        synchronized(user) {
            return user.getFinancePluginName().equals(PLUGIN_NAME);
        }
	}

	@Override
	public String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException, InternalServerErrorException {
		return this.paymentManager.getUserFinanceState(userId, provider, property);
	}

	@Override
	public void addUser(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException {
	    validateFinanceOptions(financeOptions);
        this.objectHolder.registerUser(userId, provider, PLUGIN_NAME, financeOptions);
	}
	
	@Override
	public void removeUser(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
	    this.objectHolder.removeUser(userId, provider);
	}

	@Override
	public void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException {
	    validateFinanceOptions(financeOptions);
		this.objectHolder.changeOptions(userId, provider, financeOptions);
	}

	@Override
	public void updateFinanceState(String userId, String provider, Map<String, String> financeState) throws InvalidParameterException, InternalServerErrorException {
	    FinanceUser user = this.objectHolder.getUserById(userId, provider);
	    
        synchronized (user) {
            List<Invoice> invoices = user.getInvoices();

            for (String invoiceId : financeState.keySet()) {
                for (Invoice invoice : invoices) {
                    if (invoice.getInvoiceId().equals(invoiceId)) {
                        invoice.setState(InvoiceState.fromValue(financeState.get(invoiceId)));
                    }
                }
            }
		    
            this.objectHolder.saveUser(user);
		}
	}
	
    private void validateFinanceOptions(Map<String, String> financeOptions) throws InvalidParameterException {
        if (!financeOptions.keySet().contains(PaymentRunner.USER_BILLING_INTERVAL)) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.MISSING_FINANCE_OPTION, PaymentRunner.USER_BILLING_INTERVAL));
        }

        String userBillingInterval = financeOptions.get(PaymentRunner.USER_BILLING_INTERVAL);

        try {
            Long.valueOf(userBillingInterval);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException(
                    String.format(Messages.Exception.INVALID_FINANCE_OPTION, userBillingInterval, PaymentRunner.USER_BILLING_INTERVAL));
        }
    }
}
