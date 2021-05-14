package cloud.fogbow.fs.core.plugins.finance.prepaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.PaymentManager;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

public class PrePaidFinancePluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NAME_1 = "userName1";
	private static final String USER_NOT_MANAGED = "userNotManaged";
	private static final String PROVIDER_USER_1 = "providerUser1";
	private static final String PROVIDER_USER_2 = "providerUser2";
	private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
	private InMemoryFinanceObjectsHolder objectHolder;
	private AccountingServiceClient accountingServiceClient;
	private RasClient rasClient;
	private PaymentManager paymentManager;
	private long creditsDeductionWaitTime = 1L;
    private UserCredits userCredits;
	
    // test case: When calling the managesUser method, it must
    // get the user from the objects holder and check if the user
    // is managed by the plugin.
	@Test
	public void testManagesUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        FinanceUser financeUser1 = new FinanceUser();
        financeUser1.setUserId(USER_ID_1, PROVIDER_USER_1);
        financeUser1.setFinancePluginName(PrePaidFinancePlugin.PLUGIN_NAME);

        FinanceUser financeUser2 = new FinanceUser();
        financeUser2.setUserId(USER_ID_2, PROVIDER_USER_2);
        financeUser2.setFinancePluginName(PrePaidFinancePlugin.PLUGIN_NAME);

        FinanceUser financeUser3 = new FinanceUser();
        financeUser3.setUserId(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED);
        financeUser3.setFinancePluginName("otherplugin");

        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        Mockito.when(objectHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(financeUser2);
        Mockito.when(objectHolder.getUserById(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)).thenReturn(financeUser3);

        PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, accountingServiceClient,
                rasClient, paymentManager, creditsDeductionWaitTime);

        assertTrue(prePaidFinancePlugin.managesUser(USER_ID_1, PROVIDER_USER_1));
        assertTrue(prePaidFinancePlugin.managesUser(USER_ID_2, PROVIDER_USER_2));
        assertFalse(prePaidFinancePlugin.managesUser(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user financial state is good, 
	// the method must return true.
	@Test
	public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for an
	// operation other than creation and the user financial state is not good, 
	// the method must return true.
	@Test
	public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the isAuthorized method for a
	// creation operation and the user financial state is not good, 
	// the method must return false.
	@Test
	public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
		this.paymentManager = Mockito.mock(PaymentManager.class);
		Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, 
				accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
		
		SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
		
		assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
	}
	
	// test case: When calling the addUser method, it must call the DatabaseManager
	// to create the user, create a UserCredits instance for the new user and 
	// save the user credits using the DatabaseManager.
	@Test
	public void testAddUser() throws InternalServerErrorException, InvalidParameterException {
	    this.userCredits = Mockito.mock(UserCredits.class);
	    this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
	    
	    PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, 
                accountingServiceClient, rasClient, paymentManager, creditsDeductionWaitTime);
	    
	    Map<String, String> financeOptions = new HashMap<String, String>();
	    
	    
        prePaidFinancePlugin.addUser(USER_ID_1, PROVIDER_USER_1, financeOptions);
        
        
        Mockito.verify(objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, 
                PrePaidFinancePlugin.PLUGIN_NAME, financeOptions);
	}
	
	// test case: When calling the updateFinanceState method, it must get 
	// the UserCredits for the given user, then update and save the credits state.
	@Test
	public void testUpdateFinanceState() throws InvalidParameterException, InternalServerErrorException {
	    this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        FinanceUser user = Mockito.mock(FinanceUser.class);
        Mockito.when(user.getCredits()).thenReturn(userCredits);
        
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(PrePaidFinancePlugin.CREDITS_TO_ADD, "10.5");
        
        PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, accountingServiceClient,
                rasClient, paymentManager, creditsDeductionWaitTime);

        
        prePaidFinancePlugin.updateFinanceState(USER_ID_1, PROVIDER_USER_1, financeState);

        
        Mockito.verify(userCredits).addCredits(10.5);
	}
	
	// test case: When calling the updateFinanceState method and 
	// a required state property is missing, it must throw an 
	// InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateMissingFinanceStateProperty() throws InvalidParameterException, InternalServerErrorException {
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        
        PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, accountingServiceClient,
                rasClient, paymentManager, creditsDeductionWaitTime);

        
        prePaidFinancePlugin.updateFinanceState(USER_ID_1, PROVIDER_USER_1, financeState);
    }
    
    // test case: When calling the updateFinanceState method and
    // a required state property has an invalid value, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateInvalidFinanceStateProperty() throws InvalidParameterException, InternalServerErrorException {
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(PrePaidFinancePlugin.CREDITS_TO_ADD, "invalidproperty");
        
        PrePaidFinancePlugin prePaidFinancePlugin = new PrePaidFinancePlugin(objectHolder, accountingServiceClient,
                rasClient, paymentManager, creditsDeductionWaitTime);
    
        
        prePaidFinancePlugin.updateFinanceState(USER_ID_1, PROVIDER_USER_1, financeState);
    }
}
