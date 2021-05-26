package cloud.fogbow.fs.core.plugins.plan.prepaid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

public class PrePaidPlanPluginTest {
    private static final String USER_ID_1 = "userId1";
    private static final String USER_ID_2 = "userId2";
    private static final String USER_NAME_1 = "userName1";
    private static final String USER_NAME_2 = "userName2";
    private static final String USER_NOT_MANAGED = "userNotManaged";
    private static final String PROVIDER_USER_1 = "providerUser1";
    private static final String PROVIDER_USER_2 = "providerUser2";
    private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
    private static final String PLAN_NAME = "pluginName";
    private InMemoryUsersHolder objectHolder;
    private AccountingServiceClient accountingServiceClient;
    private RasClient rasClient;
    private CreditsManager paymentManager;
    private long creditsDeductionWaitTime = 1L;
    private UserCredits userCredits;
    
    // test case: When calling the managesUser method, it must
    // get the user from the objects holder and check if the user
    // is managed by the plugin.
    @Test
    public void testManagesUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        FinanceUser financeUser1 = new FinanceUser();
        financeUser1.setUserId(USER_ID_1, PROVIDER_USER_1);
        financeUser1.setFinancePluginName(PLAN_NAME);

        FinanceUser financeUser2 = new FinanceUser();
        financeUser2.setUserId(USER_ID_2, PROVIDER_USER_2);
        financeUser2.setFinancePluginName(PLAN_NAME);

        FinanceUser financeUser3 = new FinanceUser();
        financeUser3.setUserId(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED);
        financeUser3.setFinancePluginName("otherplugin");

        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        Mockito.when(objectHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(financeUser2);
        Mockito.when(objectHolder.getUserById(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)).thenReturn(financeUser3);

        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);

        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
        assertTrue(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2)));
        assertFalse(prePaidFinancePlugin.isRegisteredUser(new SystemUser(USER_NOT_MANAGED, USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user financial state is not good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is not good, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.paymentManager = Mockito.mock(CreditsManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(prePaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the addUser method, it must call the DatabaseManager
    // to create the user, create a UserCredits instance for the new user and 
    // save the user credits using the DatabaseManager.
    @Test
    public void testAddUser() throws InternalServerErrorException, InvalidParameterException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);
        
        prePaidFinancePlugin.registerUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, PLAN_NAME);
    }
    
    // test case: When calling the updateFinanceState method, it must get 
    // the UserCredits for the given user, then update and save the credits state.
    @Test
    public void testUpdateFinanceState() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        FinanceUser user = Mockito.mock(FinanceUser.class);
        Mockito.when(user.getCredits()).thenReturn(userCredits);
        
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(PrePaidPlanPlugin.CREDITS_TO_ADD, "10.5");
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);

        
        prePaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);

        
        Mockito.verify(userCredits).addCredits(10.5);
    }
    
    // test case: When calling the updateFinanceState method and 
    // a required state property is missing, it must throw an 
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateMissingFinanceStateProperty() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);

        
        prePaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);
    }
    
    // test case: When calling the updateFinanceState method and
    // a required state property has an invalid value, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinanceStateInvalidFinanceStateProperty() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PrePaidPlanPlugin.CREDITS_DEDUCTION_WAIT_TIME, String.valueOf(creditsDeductionWaitTime));
        
        this.userCredits = Mockito.mock(UserCredits.class);
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        Map<String, String> financeState = new HashMap<String, String>();
        financeState.put(PrePaidPlanPlugin.CREDITS_TO_ADD, "invalidproperty");
        
        PrePaidPlanPlugin prePaidFinancePlugin = new PrePaidPlanPlugin(PLAN_NAME, objectHolder, accountingServiceClient,
                rasClient, paymentManager, financeOptions);
    
        
        prePaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);
    }
}
