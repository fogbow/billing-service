package cloud.fogbow.fs.core.plugins.finance.postpaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

public class PostPaidFinancePluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NAME_1 = "userName1";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
	private static final String USER_NOT_MANAGED = "userNotManaged";
	private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
    private static final String USER_BILLING_INTERVAL_1 = "10";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
	private AccountingServiceClient accountingServiceClient;
	private RasClient rasClient;
	private PaymentManager paymentManager;
	private long invoiceWaitTime = 1L;
    private Map<String, String> financeOptions;
    private Map<String, String> financeState;
    private InMemoryFinanceObjectsHolder objectHolder;

	// test case: When calling the managesUser method, it must
	// get the user from the objects holder and check if the user
    // is managed by the plugin.
	@Test
	public void testManagesUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
		FinanceUser financeUser1 = new FinanceUser();
		financeUser1.setUserId(USER_ID_1, PROVIDER_USER_1);
		financeUser1.setFinancePluginName(PostPaidFinancePlugin.PLUGIN_NAME);
		
		FinanceUser financeUser2 = new FinanceUser();
		financeUser1.setUserId(USER_ID_2, PROVIDER_USER_2);
		financeUser2.setFinancePluginName(PostPaidFinancePlugin.PLUGIN_NAME);
		
        FinanceUser financeUser3 = new FinanceUser();
        financeUser1.setUserId(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED);
        financeUser3.setFinancePluginName("otherplugin");
		
		this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
		Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
		Mockito.when(objectHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(financeUser2);
		Mockito.when(objectHolder.getUserById(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)).thenReturn(financeUser3);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		assertTrue(postPaidFinancePlugin.managesUser(USER_ID_1, PROVIDER_USER_1));
		assertTrue(postPaidFinancePlugin.managesUser(USER_ID_2, PROVIDER_USER_2));
		assertFalse(postPaidFinancePlugin.managesUser(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user financial state is good, 
	// the method must return true.
	@Test
	public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for an
	// operation other than creation and the user financial state is not good, 
	// the method must return true.
	@Test
	public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user financial state is not good, 
	// the method must return false.
	@Test
	public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertFalse(postPaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the addUser method, it must call the DatabaseManager
	// to register the user using given parameters.
	@Test
	public void testAddUser() throws InvalidParameterException, InternalServerErrorException {
	    objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
	    
	    financeOptions = new HashMap<String, String>();
	    financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL_1);

	    PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
	            accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
	    
	    
	    postPaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
	    
	    
	    Mockito.verify(this.objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, 
	            PostPaidFinancePlugin.PLUGIN_NAME, financeOptions);
	}
	
	// test case: When calling the addUser method and the finance options map 
	// does not contain some required financial option, it must throw an
	// InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testAddUserMissingOption() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        financeOptions = new HashMap<String, String>();

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the addUser method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testAddUserInvalidOption() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, "invalidvalue");

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the changeOptions method, it must call the DatabaseManager
    // to change the options for the given user.
    @Test
    public void testChangeOptions() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL_1);

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
        
        
        Mockito.verify(this.objectHolder).changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the changeOptions method and the finance options map 
    // does not contain some required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testChangeOptionsMissingOption() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        financeOptions = new HashMap<String, String>();

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the changeOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testChangeOptionsInvalidOption() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, "invalidoption");

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.changeOptions(USER_ID_1, PROVIDER_USER_1, financeOptions);
    }
    
    // test case: When calling the updateFinanceState method, it must get the correct invoices
    // from the database, change the invoices states and save the invoices.
    @Test
    public void testUpdateFinanceState() throws InvalidParameterException, InternalServerErrorException {
        Invoice invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getInvoiceId()).thenReturn(INVOICE_ID_1);
        
        Invoice invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getInvoiceId()).thenReturn(INVOICE_ID_2);
        
        ArrayList<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(invoice1);
        invoices.add(invoice2);
        
        FinanceUser user = Mockito.mock(FinanceUser.class);
        
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        Mockito.when(user.getInvoices()).thenReturn(invoices);
        
        financeState = new HashMap<String, String>();
        financeState.put(INVOICE_ID_1, InvoiceState.PAID.getValue());
        financeState.put(INVOICE_ID_2, InvoiceState.DEFAULTING.getValue());

        PostPaidFinancePlugin postPaidFinancePlugin = new PostPaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, invoiceWaitTime);
        
        
        postPaidFinancePlugin.updateFinanceState(USER_ID_1, PROVIDER_USER_1, financeState);
        
        
        Mockito.verify(invoice1).setState(InvoiceState.PAID);
        Mockito.verify(invoice2).setState(InvoiceState.DEFAULTING);
    }
}
