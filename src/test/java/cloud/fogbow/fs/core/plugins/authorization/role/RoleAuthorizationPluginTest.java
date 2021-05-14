package cloud.fogbow.fs.core.plugins.authorization.role;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.policy.XMLRolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;


@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesHolder.class)
public class RoleAuthorizationPluginTest {
    
    /*
     * user 1 can perform getFinanceState, but not addUser or reload operations
     * user 2 can perform getFinanceState and addUser, but not reload operations
     * userWithDefaultRole can perform getFinanceState, but not addUser or reload operations
     */
    private String policyFileName = "policy.xml";
    private String policyFilePath = HomeDir.getPath() + policyFileName;
    
    private String newPolicyString = "policy";

    private String expectedPolicyType = "role";
    private String wrongPolicyType = "provider";
    
    private String identityProviderId = "provider";
    
    private String userId1 = "userId1";
    private String userId2 = "userId2";
    private String userIdWithDefaultRoles = "userIdWithDefaultRole";
    
    private String userName1 = "user1";
    private String userName2 = "user2";
    private String userWithDefaultRole = "user3";
        
    private String userId1Pair = String.format(RoleAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userId1, identityProviderId);
    private String userId2Pair = String.format(RoleAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userId2, identityProviderId);
    private String userIdDefaultRolesPair = String.format(RoleAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userIdWithDefaultRoles, identityProviderId);
    
    private RoleAuthorizationPlugin plugin;
    private PropertiesHolder propertiesHolder;

    private FsOperation operationGetFinanceState;
    private FsOperation operationAddUser;
    private FsOperation operationReload;
    
    private PolicyInstantiator policyInstantiator;
    private XMLRolePolicy<FsOperation> rolePolicy;
    private XMLRolePolicy<FsOperation> newRolePolicy;
    private XMLRolePolicy<FsOperation> updatedRolePolicy;
    
    @Before
    public void setUp() throws ConfigurationErrorException, WrongPolicyTypeException {
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        this.propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(policyFileName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        Mockito.doReturn(identityProviderId).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        // set up PolicyInstantiator
        this.policyInstantiator = Mockito.mock(PolicyInstantiator.class);
        this.rolePolicy = Mockito.mock(XMLRolePolicy.class);
        Mockito.when(this.policyInstantiator.getRolePolicyInstanceFromFile(policyFilePath)).thenReturn(rolePolicy);
        
        // set up operations
        this.operationGetFinanceState = new FsOperation(OperationType.GET_FINANCE_STATE);
        this.operationAddUser = new FsOperation(OperationType.ADD_USER);
        this.operationReload = new FsOperation(OperationType.RELOAD);
        
        // set up RolePolicy
        Mockito.when(this.rolePolicy.userIsAuthorized(userId1Pair, operationGetFinanceState)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId1Pair, operationAddUser)).thenReturn(false);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId1Pair, operationReload)).thenReturn(false);
        
        Mockito.when(this.rolePolicy.userIsAuthorized(userId2Pair, operationGetFinanceState)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId2Pair, operationAddUser)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId2Pair, operationReload)).thenReturn(false);
        
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdDefaultRolesPair, operationGetFinanceState)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdDefaultRolesPair, operationAddUser)).thenReturn(false);
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdDefaultRolesPair, operationReload)).thenReturn(false);

        this.plugin = new RoleAuthorizationPlugin(this.policyInstantiator);
    }

    // test case: When creating a new instance of RoleAuthorizationPlugin, the constructor 
    // must read the policy file name, load the correct role policy and validate the policy. 
    @Test
    public void constructorReadsConfigurationCorrectly() throws ConfigurationErrorException {
        Mockito.verify(this.rolePolicy, Mockito.atLeastOnce()).validate();
        
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        PowerMockito.verifyStatic(PropertiesHolder.class, Mockito.atLeastOnce());
    }
    
    // test case: When creating a new instance of RoleAuthorizationPlugin and 
    // the PolicyInstantiator throws a WrongPolicyTypeException, the constructor
    // must rethrow the exception.
    @Test(expected = ConfigurationErrorException.class)
    public void constructorThrowsExceptionIfPolicyTypeIsWrong() throws ConfigurationErrorException, WrongPolicyTypeException {
        this.policyInstantiator = Mockito.mock(PolicyInstantiator.class);
        Mockito.when(this.policyInstantiator.getRolePolicyInstanceFromFile(policyFilePath)).
        thenThrow(new WrongPolicyTypeException("", ""));
        
        new RoleAuthorizationPlugin(this.policyInstantiator);
    }

    // test case: When calling the isAuthorized method, it must call the RolePolicy 
    // userIsAuthorized method to determine whether or not the user has permission to 
    // perform the given operation.
    // In this test:
    // user 1 can perform getFinanceState, but not addUser or reload operations
    // user 2 can perform getFinanceState and addUser, but not reload operations
    // userWithDefaultRole can perform getFinanceState, but not addUser or reload operations
    @Test
    public void testIsAuthorized() throws UnauthorizedRequestException {
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        assertIsAuthorizedThrowsException(user1, operationAddUser);
        assertIsAuthorizedThrowsException(user1, operationReload);

        SystemUser user2 = new SystemUser(userId2, userName2, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user2, operationGetFinanceState));
        assertTrue(this.plugin.isAuthorized(user2, operationAddUser));
        assertIsAuthorizedThrowsException(user2, operationReload);
        
        SystemUser userWithDefaultRoles = new SystemUser(userIdWithDefaultRoles, userWithDefaultRole, identityProviderId);

        assertTrue(this.plugin.isAuthorized(userWithDefaultRoles, operationGetFinanceState));
        assertIsAuthorizedThrowsException(userWithDefaultRoles, operationAddUser);
        assertIsAuthorizedThrowsException(userWithDefaultRoles, operationReload);
    }
    
    // test case: when calling the setPolicy method with a valid policy string, 
    // it must call PolicyInstantiator to create a new policy instance, validate
    // and persist the new instance and use this new instance to authorize operations.
    @Test
    public void testSetPolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        Mockito.when(this.newRolePolicy.userIsAuthorized(userId1Pair, operationGetFinanceState)).thenReturn(false);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(newRolePolicy);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        
        this.plugin.setPolicy(newPolicyString);
        
        assertIsAuthorizedThrowsException(user1, operationGetFinanceState);
        Mockito.verify(this.newRolePolicy, Mockito.atLeastOnce()).validate();
        Mockito.verify(this.newRolePolicy, Mockito.atLeastOnce()).save();
    }
    
    // test case: when calling the setPolicy method with a wrong policy type string,
    // it must handle the WrongPolicyTypeException and not change the policy it uses
    // to authorize operations.
    @Test
    public void testSetPolicyWrongPolicyType() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        WrongPolicyTypeException exception = new WrongPolicyTypeException(expectedPolicyType, wrongPolicyType);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenThrow(exception);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        
        this.plugin.setPolicy(newPolicyString);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        Mockito.verify(this.newRolePolicy, Mockito.never()).validate();
    }
    
    // test case: when calling the setPolicy method and the validation
    // fails, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testSetInvalidPolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        ConfigurationErrorException exception = new ConfigurationErrorException();
        Mockito.when(this.newRolePolicy.userIsAuthorized(userId1Pair, operationGetFinanceState)).thenReturn(false);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(newRolePolicy);
        Mockito.doThrow(exception).when(this.newRolePolicy).validate();
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        
        this.plugin.setPolicy(newPolicyString);
    }
    
    // test case: when calling the updatePolicy method with a valid policy string, 
    // it must call PolicyInstantiator to create a new policy instance from the policy string, 
    // create a copy of the policy it uses, update, validate and save this copy. Then, use this
    // new instance to authorize operations.
    @Test
    public void testUpdatePolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        this.updatedRolePolicy = Mockito.mock(XMLRolePolicy.class);
        Mockito.when(this.rolePolicy.copy()).thenReturn(updatedRolePolicy);
        Mockito.when(this.updatedRolePolicy.userIsAuthorized(userId1Pair, operationGetFinanceState)).thenReturn(false);
        
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(this.newRolePolicy);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        
        this.plugin.updatePolicy(newPolicyString);
        
        assertIsAuthorizedThrowsException(user1, operationGetFinanceState);

        Mockito.verify(this.updatedRolePolicy, Mockito.atLeastOnce()).update(this.newRolePolicy);
        Mockito.verify(this.updatedRolePolicy, Mockito.atLeastOnce()).validate();
        Mockito.verify(this.updatedRolePolicy, Mockito.atLeastOnce()).save();
    }
    
    // test case: when calling the updatePolicy method with a wrong policy type string,
    // it must handle the WrongPolicyTypeException and not change the policy it uses
    // to authorize operations.
    @Test
    public void testUpdatePolicyWrongPolicyType() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        WrongPolicyTypeException exception = new WrongPolicyTypeException(expectedPolicyType, wrongPolicyType);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenThrow(exception);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        
        this.plugin.updatePolicy(newPolicyString);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
    }
    
    // test case: when calling the updatePolicy method and the validation
    // fails, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testUpdateInvalidPolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        this.updatedRolePolicy = Mockito.mock(XMLRolePolicy.class);
        ConfigurationErrorException exception = new ConfigurationErrorException();
        
        Mockito.when(this.rolePolicy.copy()).thenReturn(updatedRolePolicy);
        Mockito.doThrow(exception).when(this.updatedRolePolicy).validate();
        
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(this.newRolePolicy);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGetFinanceState));
        
        this.plugin.updatePolicy(newPolicyString);
    }
    
    private void assertIsAuthorizedThrowsException(SystemUser user, FsOperation operation) {
        try {
            this.plugin.isAuthorized(user, operation);
            Assert.fail("isAuthorized call should fail.");
        } catch (UnauthorizedRequestException e) {

        }
    }
}
