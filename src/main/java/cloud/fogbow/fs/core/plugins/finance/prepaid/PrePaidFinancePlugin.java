package cloud.fogbow.fs.core.plugins.finance.prepaid;

import java.util.Map;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.plugins.PaymentManagerInstantiator;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class PrePaidFinancePlugin implements FinancePlugin {
	/**
	 * A textual reference to this plugin. 
	 * Normally used as a user property to set this class
	 * as the one to be used to manage the user.
	 */
	public static final String PLUGIN_NAME = "prepaid";
	/**
	 * The key to use in the configuration property which 
	 * indicates the {@link cloud.fogbow.fs.core.plugins.PaymentManager}
	 * to use.
	 * Required in the configuration file. 
	 */
	public static final String PRE_PAID_PAYMENT_MANAGER = "pre_paid_payment_manager";
	/**
     * The key to use in the configuration property which
     * indicates the {@link cloud.fogbow.fs.core.models.FinancePlan}
     * to use.
     * Required in the configuration file.
     */
	public static final String PRE_PAID_DEFAULT_FINANCE_PLAN = "pre_paid_default_finance_plan";
	/**
	 * The key to use in the configuration property which
	 * indicates the delay between credits deduction attempts.
	 */
	public static final String CREDITS_DEDUCTION_WAIT_TIME = "credits_deduction_wait_time";
	/**
	 * The key to use in the map passed as argument to updateFinanceState 
	 * to indicate the value of credits to add to the user state.
	 */
    public static final String CREDITS_TO_ADD = "CREDITS_TO_ADD";
	
	private Thread paymentThread;
	private Thread stopServiceThread;
	private PaymentManager paymentManager;
	private AccountingServiceClient accountingServiceClient; 
	private RasClient rasClient;
	private PaymentRunner paymentRunner;
	private StopServiceRunner stopServiceRunner;
	private long creditsDeductionWaitTime;
	private boolean threadsAreRunning;
	private InMemoryFinanceObjectsHolder objectHolder;
	
    public PrePaidFinancePlugin(InMemoryFinanceObjectsHolder objectHolder) throws ConfigurationErrorException {
        this(objectHolder, new AccountingServiceClient(), new RasClient(),
                PaymentManagerInstantiator.getPaymentManager(
                        PropertiesHolder.getInstance().getProperty(PRE_PAID_PAYMENT_MANAGER), objectHolder, 
                        PropertiesHolder.getInstance().getProperty(PRE_PAID_DEFAULT_FINANCE_PLAN)),
                Long.valueOf(PropertiesHolder.getInstance().getProperty(CREDITS_DEDUCTION_WAIT_TIME)));
    }
	
	public PrePaidFinancePlugin(InMemoryFinanceObjectsHolder objectHolder, AccountingServiceClient accountingServiceClient,
            RasClient rasClient, PaymentManager paymentManager, long creditsDeductionWaitTime) {
        this.accountingServiceClient = accountingServiceClient;
        this.rasClient = rasClient;
        this.paymentManager = paymentManager;
        this.creditsDeductionWaitTime = creditsDeductionWaitTime;
        this.threadsAreRunning = false;
        this.objectHolder = objectHolder;
    }

    @Override
	public String getName() {
		return PLUGIN_NAME;
	}
	
	@Override
	public void startThreads() {
		if (!this.threadsAreRunning) {
			this.paymentRunner = new PaymentRunner(creditsDeductionWaitTime, objectHolder, 
					accountingServiceClient, paymentManager);
			this.paymentThread = new Thread(paymentRunner);
			
			this.stopServiceRunner = new StopServiceRunner(creditsDeductionWaitTime, objectHolder, 
					paymentManager, rasClient);
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
	public void addUser(String userId, String provider, Map<String, String> financeOptions) throws InternalServerErrorException, InvalidParameterException {
        this.objectHolder.registerUser(userId, provider, PLUGIN_NAME, financeOptions);
	}

	@Override
	public void removeUser(String userId, String provider) throws InvalidParameterException, InternalServerErrorException {
        this.objectHolder.removeUser(userId, provider);
	}

	@Override
	public void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException {
		this.objectHolder.changeOptions(userId, provider, financeOptions);
	}

	@Override
	public void updateFinanceState(String userId, String provider, Map<String, String> financeState) throws InvalidParameterException, 
	InternalServerErrorException {
	    if (!financeState.containsKey(CREDITS_TO_ADD)) {
	        throw new InvalidParameterException(String.format(Messages.Exception.MISSING_FINANCE_STATE_PROPERTY, CREDITS_TO_ADD));
	    }
	    
	    Double valueToAdd = 0.0;
	    String propertyValue = "";
	    
	    try {
	        propertyValue = financeState.get(CREDITS_TO_ADD);
	        valueToAdd = Double.valueOf(propertyValue);
	    } catch (NumberFormatException e) {
	        throw new InvalidParameterException(String.format(Messages.Exception.INVALID_FINANCE_STATE_PROPERTY, propertyValue, CREDITS_TO_ADD));
	    }
	    
	    FinanceUser user = this.objectHolder.getUserById(userId, provider);
	    
	    
	    synchronized(user) {
	        UserCredits userCredits = user.getCredits();
	        userCredits.addCredits(valueToAdd);
	        this.objectHolder.saveUser(user);
	    }
	}
}
