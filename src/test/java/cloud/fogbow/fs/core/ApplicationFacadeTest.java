package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.core.models.OperationType;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;
import cloud.fogbow.fs.core.util.SynchronizationManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FsPublicKeysHolder.class, AuthenticationUtil.class, 
    ServiceAsymmetricKeysHolder.class, CryptoUtil.class, 
    AuthorizationPluginInstantiator.class, PropertiesHolder.class})
public class ApplicationFacadeTest {

	private static final String PRIVATE_KEY_FILEPATH_AFTER_RELOAD = "privatekeyfilepathafter";
    private static final String PUBLIC_KEY_FILEPATH_AFTER_RELOAD = "publickeyfilepathafter";
    private String adminId = "adminId";
	private String adminUserName = "adminUserName";
	private String adminProvider = "adminProvider";
	private String adminToken = "token";
	
	private String userIdToAdd = "userIdToAdd";
	private String userProviderToAdd = "userProviderToAdd";
	private String financePluginUserToAdd = "financePluginUserToAdd";
	private Map<String, String> userFinanceOptionsToAdd = new HashMap<String, String>();

	private String userIdToRemove = "userIdToRemove";
	private String userProviderToRemove = "userProviderToRemove";
	
	private String userIdToChange = "userIdToChange";
	private String userProviderToChange = "userProviderToChange";
	private HashMap<String, String> newOptions = new HashMap<String, String>();
	private HashMap<String, String> newState = new HashMap<String, String>();
	private String property = "property";
	private String propertyValue = "propertyValue";
	
	private String newPlanName = "newPlanName";
	private Map<String, String> newPlanInfo = new HashMap<String, String>();
	
	private String planToUpdate = "planToUpdate";
	private Map<String, String> updatedPlanInfo = new HashMap<String, String>();
	
	private String planToRemove = "planToRemove";
	
	private String newPolicy = "newPolicy";
	
	private FsPublicKeysHolder keysHolder;
	private RSAPublicKey asPublicKey;
	private User user;
	private FinanceManager financeManager;
	private SynchronizationManager synchronizationManager;
	private SystemUser systemUser;
	private FsOperation operation;
	private AuthorizationPlugin<FsOperation> authorizationPlugin;

	// test case: When calling the addUser method, it must authorize the 
	// operation and call the FinanceManager. Also, it must start and finish 
	// operations correctly using the SynchronizationManager.
	@Test
	public void testAddUser() throws FogbowException {
		setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.ADD_USER);
        setUpApplicationFacade();
        
        this.user = new User(userIdToAdd, userProviderToAdd, financePluginUserToAdd, userFinanceOptionsToAdd);
        
        ApplicationFacade.getInstance().addUser(adminToken, user);
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
        Mockito.verify(financeManager, Mockito.times(1)).addUser(user);
	}
	
	// test case: When calling the addUser method, if the call to 
	// FinanceManager.addUser throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testAddUserFinishesOperationIfOperationFails() throws FogbowException  {
		setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.ADD_USER);
        
        this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);
        
        ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
        ApplicationFacade.getInstance().setFinanceManager(financeManager);
        ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);
        
        this.user = new User(userIdToAdd, userProviderToAdd, financePluginUserToAdd, userFinanceOptionsToAdd);
        Mockito.doThrow(FogbowException.class).when(this.financeManager).addUser(user);
        
        try {
			ApplicationFacade.getInstance().addUser(adminToken, user);
			Assert.fail("addUser is expected to throw exception.");
		} catch (FogbowException e) {
		}
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}
	
	// test case: When calling the removeUser method, it must authorize the
	// operation and call the FinanceManager. Also, it must start and finish
	// operations correctly using the SynchronizationManager.
	@Test
	public void testRemoveUser() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization(OperationType.REMOVE_USER);
		setUpApplicationFacade();

		ApplicationFacade.getInstance().removeUser(adminToken, this.userIdToRemove, this.userProviderToRemove);

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
		Mockito.verify(financeManager, Mockito.times(1)).removeUser(this.userIdToRemove, this.userProviderToRemove);
	}
	
	// test case: When calling the removeUser method, if the call to
	// FinanceManager.removeUser throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testRemoveUserFinishesOperationIfOperationFails() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization(OperationType.REMOVE_USER);

		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);

		ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);

		Mockito.doThrow(FogbowException.class).when(this.financeManager).removeUser(userIdToRemove, 
				this.userProviderToRemove);

		try {
			ApplicationFacade.getInstance().removeUser(adminToken, userIdToRemove, this.userProviderToRemove);
			Assert.fail("removeUser is expected to throw exception.");
		} catch (FogbowException e) {
		}

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
		Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}

	// test case: When calling the changeOptions method, it must authorize the
	// operation and call the FinanceManager. Also, it must start and finish
	// operations correctly using the SynchronizationManager.
	@Test
	public void testChangeOptions() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization(OperationType.CHANGE_OPTIONS);
		setUpApplicationFacade();

		ApplicationFacade.getInstance().changeOptions(adminToken, this.userIdToChange, 
				this.userProviderToChange, this.newOptions);

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
		Mockito.verify(financeManager, Mockito.times(1)).changeOptions(this.userIdToChange, 
				this.userProviderToChange, this.newOptions);
	}
	
	// test case: When calling the changeOptions method, if the call to
	// FinanceManager.changeOptions throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testChangeOptionsFinishesOperationIfOperationFails() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization(OperationType.CHANGE_OPTIONS);

		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);

		ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);

		Mockito.doThrow(FogbowException.class).when(this.financeManager).changeOptions(this.userIdToChange, 
				this.userProviderToChange, this.newOptions);

		try {
			ApplicationFacade.getInstance().changeOptions(this.adminToken, this.userIdToChange, 
					this.userProviderToChange, this.newOptions);
			Assert.fail("changeOptions is expected to throw exception.");
		} catch (FogbowException e) {
		}

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
		Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}
	
	// test case: When calling the updateFinanceState method, it must authorize the
	// operation and call the FinanceManager. Also, it must start and finish
	// operations correctly using the SynchronizationManager.
	@Test
	public void testUpdateFinanceState() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization(OperationType.UPDATE_FINANCE_STATE);
		setUpApplicationFacade();

		ApplicationFacade.getInstance().updateFinanceState(adminToken, this.userIdToChange, 
				this.userProviderToChange, this.newState);

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
		Mockito.verify(financeManager, Mockito.times(1)).updateFinanceState(this.userIdToChange,
				this.userProviderToChange, this.newState);
	}
	
	// test case: When calling the updateFinanceState method, if the call to
	// FinanceManager.updateFinanceState throws an exception, the method
	// must rethrow the exception and finish the operation correctly.
	@Test
	public void testUpdateFinanceStateFinishesOperationIfOperationFails() throws FogbowException {
		setUpPublicKeysHolder();
		setUpAuthentication();
		setUpAuthorization(OperationType.UPDATE_FINANCE_STATE);

		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);

		ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
		ApplicationFacade.getInstance().setFinanceManager(financeManager);
		ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);

		Mockito.doThrow(FogbowException.class).when(this.financeManager).updateFinanceState(this.userIdToChange, 
				this.userProviderToChange, this.newState);

		try {
			ApplicationFacade.getInstance().updateFinanceState(this.adminToken, this.userIdToChange, 
					this.userProviderToChange, this.newState);
			Assert.fail("updateFinanceState is expected to throw exception.");
		} catch (FogbowException e) {
		}

		Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
		Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
		Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
	}
	
    // test case: When calling the getFinanceStateProperty method, it must authorize the
    // operation and call the FinanceManager. Also, it must start and finish
    // operations correctly using the SynchronizationManager.
	@Test
    public void testGetFinanceStateProperty() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.GET_FINANCE_STATE);
        setUpApplicationFacade();
        
        Mockito.when(financeManager.getFinanceStateProperty(userIdToChange, 
                userProviderToChange, property)).thenReturn(propertyValue);
        
        String returnedProperty = ApplicationFacade.getInstance().getFinanceStateProperty(adminToken, 
                userIdToChange, userProviderToChange, property);
        
        assertEquals(propertyValue, returnedProperty);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
	
    // test case: When calling the getFinanceStateProperty method, if the call to
    // FinanceManager.getFinanceStateProperty throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testGetFinanceStatePropertyFinishesOperationIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.GET_FINANCE_STATE);
        setUpApplicationFacade();
        
        Mockito.when(financeManager.getFinanceStateProperty(userIdToChange, 
                userProviderToChange, property)).thenThrow(new FogbowException("message"));
        
        try {
            ApplicationFacade.getInstance().getFinanceStateProperty(adminToken, 
                    userIdToChange, userProviderToChange, property);
            Assert.fail("getFinanceStateProperty is expected to throw exception.");
        } catch (FogbowException e) {
        }
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
	
	// test case: When calling the isAuthorized method, it must call the isAuthorized
	// method of the FinanceManager and start and finish operations correctly using
	// the SynchronizationManager.
	@Test
	public void testIsAuthorized() throws FogbowException {
	    setUpPublicKeysHolder();
        setUpApplicationFacade();
        
        Boolean authorized = true;
        AuthorizableUser authorizableUser = Mockito.mock(AuthorizableUser.class);
        Mockito.when(financeManager.isAuthorized(authorizableUser)).thenReturn(authorized);

        Boolean returnedAuthorized = ApplicationFacade.getInstance().isAuthorized(authorizableUser);
        assertEquals(authorized, returnedAuthorized);
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(this.financeManager).isAuthorized(authorizableUser);
	}
	
	// test case: When calling the isAuthorized method, if the call to
    // FinanceManager.isAuthorized throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testIsAuthorizedFinishesOperationIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpApplicationFacade();

        AuthorizableUser authorizableUser = Mockito.mock(AuthorizableUser.class);

        Mockito.doThrow(FogbowException.class).when(this.financeManager).isAuthorized(authorizableUser);            

        try {
            ApplicationFacade.getInstance().isAuthorized(authorizableUser);
            Assert.fail("isAuthorized is expected to throw exception.");
        } catch (FogbowException e) {
        }
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(this.financeManager).isAuthorized(authorizableUser);
    }
    
    // test case: When calling the getPublicKey method, it must call the 
    // ServiceAsymmetricKeysHolder to get the public key and start and finish 
    // operations correctly using the SynchronizationManager.
    @Test
    public void testGetPublicKey() throws FogbowException, GeneralSecurityException {
        setUpPublicKeysHolder();
        setUpApplicationFacade();
        
        RSAPublicKey rsaPublicKey = Mockito.mock(RSAPublicKey.class);
        String publicKeyString = "publicKey";
        
        ServiceAsymmetricKeysHolder asymmetricKeysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.when(asymmetricKeysHolder.getPublicKey()).thenReturn(rsaPublicKey);
        

        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(asymmetricKeysHolder);
        
        PowerMockito.mockStatic(CryptoUtil.class);
        BDDMockito.given(CryptoUtil.toBase64(rsaPublicKey)).willReturn(publicKeyString);
        
        
        String returnedPublicKey = ApplicationFacade.getInstance().getPublicKey();            
        
        
        assertEquals(publicKeyString, returnedPublicKey);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
    }
    
    // test case: When calling the isAuthorized method, if the call to
    // ServiceAsymmetricKeysHolder.getPublicKey throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testGetPublicKeyFinishesOperationIfOperationFailsOnKeysHolder() throws FogbowException, GeneralSecurityException {
        setUpPublicKeysHolder();
        setUpApplicationFacade();

        ServiceAsymmetricKeysHolder asymmetricKeysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.doThrow(InternalServerErrorException.class).when(asymmetricKeysHolder).getPublicKey(); 

        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(asymmetricKeysHolder);

        try {
            ApplicationFacade.getInstance().getPublicKey();
            Assert.fail("getPublicKey is expected to throw exception.");
        } catch (InternalServerErrorException e) {
        }
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
    }
    
    // test case: When calling the isAuthorized method, if the call to
    // CryptoUtil.toBase64 throws an exception, the method must catch the 
    // exception, throw an InternalServerErrorException and finish the operation correctly.
    @Test
    public void testGetPublicKeyFinishesOperationIfOperationFailsOnCryptoUtil() throws FogbowException, GeneralSecurityException {
        setUpPublicKeysHolder();
        setUpApplicationFacade();
        
        RSAPublicKey rsaPublicKey = Mockito.mock(RSAPublicKey.class);
        
        ServiceAsymmetricKeysHolder asymmetricKeysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.when(asymmetricKeysHolder.getPublicKey()).thenReturn(rsaPublicKey);
        
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(asymmetricKeysHolder);
        
        PowerMockito.mockStatic(CryptoUtil.class);
        BDDMockito.given(CryptoUtil.toBase64(rsaPublicKey)).willThrow(new GeneralSecurityException());
        
        try {
            ApplicationFacade.getInstance().getPublicKey();
            Assert.fail("getPublicKey is expected to throw exception.");
        } catch (InternalServerErrorException e) {
        }
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
    }
    
    // test case: When calling the createFinancePlan method, it must authorize the
    // operation and call the FinanceManager. Also, it must start and finish
    // operations correctly using the SynchronizationManager.
    @Test
    public void testCreateFinancePlan() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.CREATE_FINANCE_PLAN);
        setUpApplicationFacade();
        
        
        ApplicationFacade.getInstance().createFinancePlan(adminToken, newPlanName, newPlanInfo);
        
        
        Mockito.verify(financeManager, Mockito.times(1)).createFinancePlan(newPlanName, newPlanInfo);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the createFinancePlan method, if the call to
    // FinanceManager.createFinancePlan throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testCreateFinancePlanFinishesOperationIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.CREATE_FINANCE_PLAN);
        setUpApplicationFacade();
        
        Mockito.doThrow(InvalidParameterException.class).when(financeManager).createFinancePlan(newPlanName, newPlanInfo);
        
        try {
            ApplicationFacade.getInstance().createFinancePlan(adminToken, newPlanName, newPlanInfo);
            Assert.fail("createFinancePlan is expected to throw exception.");
        } catch (InvalidParameterException e) {
        }
        
        Mockito.verify(financeManager, Mockito.times(1)).createFinancePlan(newPlanName, newPlanInfo);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the getFinancePlan method, it must authorize the
    // operation and call the FinanceManager. Also, it must start and finish
    // operations correctly using the SynchronizationManager.
    @Test
    public void testGetFinancePlan() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.GET_FINANCE_PLAN);
        setUpApplicationFacade();
        
        Mockito.when(financeManager.getFinancePlan(newPlanName)).thenReturn(newPlanInfo);

        Map<String, String> returnedPlan = ApplicationFacade.getInstance().getFinancePlan(adminToken, newPlanName);
        
        assertEquals(newPlanInfo, returnedPlan);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the getFinancePlan method, if the call to
    // FinanceManager.getFinancePlan throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testGetFinancePlanFinishesOperationIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.GET_FINANCE_PLAN);
        setUpApplicationFacade();
        
        Mockito.when(financeManager.getFinancePlan(newPlanName)).thenThrow(new InvalidParameterException());

        try {
            ApplicationFacade.getInstance().getFinancePlan(adminToken, newPlanName);
            Assert.fail("getFinancePlan is expected to throw exception.");
        } catch (InvalidParameterException e) {
        }
        
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the updateFinancePlan method, it must authorize the
    // operation and call the FinanceManager. Also, it must start and finish
    // operations correctly using the SynchronizationManager.
    @Test
    public void testUpdateFinancePlan() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.UPDATE_FINANCE_PLAN);
        setUpApplicationFacade();

        
        ApplicationFacade.getInstance().updateFinancePlan(adminToken, planToUpdate, updatedPlanInfo);
        
        
        Mockito.verify(financeManager, Mockito.times(1)).updateFinancePlan(planToUpdate, updatedPlanInfo);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the updateFinancePlan method, if the call to
    // FinanceManager.updateFinancePlan throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testUpdateFinancePlanFinishesOperationIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.UPDATE_FINANCE_PLAN);
        setUpApplicationFacade();

        Mockito.doThrow(InvalidParameterException.class).when(financeManager).updateFinancePlan(planToUpdate, updatedPlanInfo);
        
        try {
            ApplicationFacade.getInstance().updateFinancePlan(adminToken, planToUpdate, updatedPlanInfo);
            Assert.fail("updateFinancePlan is expected to throw exception.");
        } catch(InvalidParameterException e) {
        }
        
        Mockito.verify(financeManager, Mockito.times(1)).updateFinancePlan(planToUpdate, updatedPlanInfo);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the removeFinancePlan method, it must authorize the
    // operation and call the FinanceManager. Also, it must start and finish
    // operations correctly using the SynchronizationManager.
    @Test
    public void testRemoveFinancePlan() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.REMOVE_FINANCE_PLAN);
        setUpApplicationFacade();

        
        ApplicationFacade.getInstance().removeFinancePlan(adminToken, planToRemove);
        
        
        Mockito.verify(financeManager, Mockito.times(1)).removeFinancePlan(planToRemove);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the removeFinancePlan method, if the call to
    // FinanceManager.removeFinancePlan throws an exception, the method
    // must rethrow the exception and finish the operation correctly.
    @Test
    public void testRemoveFinancePlanFinishesOperationIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.REMOVE_FINANCE_PLAN);
        setUpApplicationFacade();

        Mockito.doThrow(InvalidParameterException.class).when(financeManager).removeFinancePlan(planToRemove);
        
        try {
            ApplicationFacade.getInstance().removeFinancePlan(adminToken, planToRemove);
            Assert.fail("removeFinancePlan is expected to throw exception.");
        } catch(InvalidParameterException e) {
        }
        
        Mockito.verify(financeManager, Mockito.times(1)).removeFinancePlan(planToRemove);
        Mockito.verify(synchronizationManager, Mockito.times(1)).startOperation();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishOperation();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the setPolicy method, it must authorize the
    // operation and call the AuthorizationPlugin. Also, it must start and finish
    // reloading correctly using the SynchronizationManager.
    @Test
    public void testSetPolicy() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.SET_POLICY);
        setUpApplicationFacade();
        
        
        ApplicationFacade.getInstance().setPolicy(adminToken, newPolicy);
        
        
        Mockito.verify(authorizationPlugin, Mockito.times(1)).setPolicy(newPolicy);
        Mockito.verify(synchronizationManager, Mockito.times(1)).setAsReloading();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishReloading();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the setPolicy method, if the call to
    // AuthorizationPlugin.setPolicy throws an exception, the method
    // must rethrow the exception and finish the reloading correctly.
    @Test
    public void testSetPolicyFinishesReloadingIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.SET_POLICY);
        setUpApplicationFacade();
        
        Mockito.doThrow(ConfigurationErrorException.class).when(authorizationPlugin).setPolicy(newPolicy);
        
        try {
            ApplicationFacade.getInstance().setPolicy(adminToken, newPolicy);
            Assert.fail("setPolicy is expected to throw exception.");
        } catch(ConfigurationErrorException e) {
        }
        
        Mockito.verify(authorizationPlugin, Mockito.times(1)).setPolicy(newPolicy);
        Mockito.verify(synchronizationManager, Mockito.times(1)).setAsReloading();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishReloading();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the updatePolicy method, it must authorize the
    // operation and call the AuthorizationPlugin. Also, it must start and finish
    // reloading correctly using the SynchronizationManager.
    @Test
    public void testUpdatePolicy() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.UPDATE_POLICY);
        setUpApplicationFacade();
        
        
        ApplicationFacade.getInstance().updatePolicy(adminToken, newPolicy);
        
        
        Mockito.verify(authorizationPlugin, Mockito.times(1)).updatePolicy(newPolicy);
        Mockito.verify(synchronizationManager, Mockito.times(1)).setAsReloading();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishReloading();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the updatePolicy method, if the call to
    // AuthorizationPlugin.updatePolicy throws an exception, the method
    // must rethrow the exception and finish the reloading correctly.
    @Test
    public void testUpdatePolicyFinishesReloadingIfOperationFails() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.UPDATE_POLICY);
        setUpApplicationFacade();
        
        Mockito.doThrow(ConfigurationErrorException.class).when(authorizationPlugin).updatePolicy(newPolicy);
        
        try {
            ApplicationFacade.getInstance().updatePolicy(adminToken, newPolicy);
            Assert.fail("setPolicy is expected to throw exception.");
        } catch(ConfigurationErrorException e) {
        }
        
        Mockito.verify(authorizationPlugin, Mockito.times(1)).updatePolicy(newPolicy);
        Mockito.verify(synchronizationManager, Mockito.times(1)).setAsReloading();
        Mockito.verify(synchronizationManager, Mockito.times(1)).finishReloading();
        Mockito.verify(authorizationPlugin, Mockito.times(1)).isAuthorized(systemUser, operation);
    }
    
    // test case: When calling the reload method, it must stop the running services, 
    // reset all configuration and key holders and restart the services.
    @Test
    public void testReload() throws FogbowException {
        setUpPublicKeysHolder();
        setUpAuthentication();
        setUpAuthorization(OperationType.RELOAD);
        setUpApplicationFacade();
        
        PropertiesHolder propertiesHolderAfterReload = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolderAfterReload.getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH)).
                    thenReturn(PUBLIC_KEY_FILEPATH_AFTER_RELOAD);
        Mockito.when(propertiesHolderAfterReload.getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH)).
                    thenReturn(PRIVATE_KEY_FILEPATH_AFTER_RELOAD);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolderAfterReload);
        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);

        
        ApplicationFacade.getInstance().reload(adminToken);
        

        Mockito.verify(financeManager).stopPlugins();
        Mockito.verify(financeManager).resetPlugins();
        Mockito.verify(financeManager).startPlugins();
        Mockito.verify(synchronizationManager).setAsReloading();
        Mockito.verify(synchronizationManager).finishReloading();
        Mockito.verify(authorizationPlugin).isAuthorized(systemUser, operation);
        
        PowerMockito.verifyStatic(PropertiesHolder.class);
        PropertiesHolder.reset();
        
        PowerMockito.verifyStatic(FsPublicKeysHolder.class);
        FsPublicKeysHolder.reset();
        
        PowerMockito.verifyStatic(ServiceAsymmetricKeysHolder.class);
        ServiceAsymmetricKeysHolder.reset(PUBLIC_KEY_FILEPATH_AFTER_RELOAD, PRIVATE_KEY_FILEPATH_AFTER_RELOAD);
        
        PowerMockito.verifyStatic(AuthorizationPluginInstantiator.class);
        AuthorizationPluginInstantiator.getAuthorizationPlugin();
    }
	
	private void setUpApplicationFacade() {
		this.financeManager = Mockito.mock(FinanceManager.class);
		this.synchronizationManager = Mockito.mock(SynchronizationManager.class);
        
        ApplicationFacade.getInstance().setAuthorizationPlugin(authorizationPlugin);
        ApplicationFacade.getInstance().setFinanceManager(financeManager);
        ApplicationFacade.getInstance().setSynchronizationManager(synchronizationManager);
	}

	private void setUpPublicKeysHolder() throws FogbowException {
		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		this.keysHolder = Mockito.mock(FsPublicKeysHolder.class);
		this.asPublicKey = Mockito.mock(RSAPublicKey.class);
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(keysHolder);
		Mockito.when(keysHolder.getAsPublicKey()).thenReturn(asPublicKey);
	}
	
	private void setUpAuthentication() throws UnauthenticatedUserException {
		PowerMockito.mockStatic(AuthenticationUtil.class);
		this.systemUser = new SystemUser(adminId, adminUserName, adminProvider);
		BDDMockito.given(AuthenticationUtil.authenticate(asPublicKey, adminToken)).willReturn(systemUser);
	}
	
	private void setUpAuthorization(OperationType operationType) throws UnauthorizedRequestException {
		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
        this.operation = new FsOperation(operationType);
        Mockito.when(authorizationPlugin.isAuthorized(systemUser, operation)).thenReturn(true);
	}
}
