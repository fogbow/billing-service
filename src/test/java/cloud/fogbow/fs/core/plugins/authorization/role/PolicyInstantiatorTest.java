package cloud.fogbow.fs.core.plugins.authorization.role;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.FsClassFactory;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;


@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, HomeDir.class})
public class PolicyInstantiatorTest {

    private static final String ADMIN_ROLE = "admin";
    private static final String POLICY_FILE = "policy_file";
    private static final String POLICY_CLASS = "policy_class";
    private static final String HOME_DIR_PATH = "home_dir_path/";
    private static final String POLICY_STRING = "policy";
    private FsClassFactory classFactory;
    private Properties properties;
    private PropertiesHolder propertiesHolder;
    private PermissionInstantiator<FsOperation> permissionInstantiator;
    private RolePolicy<FsOperation> policy;

    // test case: When creating a new PolicyInstantiator instance, the constructor
    // must get the required configuration from the PropertiesHolder.
    @Test
    public void testConstructorReadsConfiguration() throws ConfigurationErrorException {
        setUpConfiguration();
        

        new PolicyInstantiator(classFactory, permissionInstantiator);
        
        
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
        
        PowerMockito.verifyStatic(HomeDir.class);
        HomeDir.getPath();
    }

    // test case: When creating a new PolicyInstantiator instance and the configuration
    // does not specify an admin role, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorConfigurationDoesNotSpecifyAdminRole() throws ConfigurationErrorException {
        setUpConfiguration();
        
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(false);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(null);

        new PolicyInstantiator(classFactory, permissionInstantiator);
    }
    
    // test case: When creating a new PolicyInstantiator instance and the configuration
    // does not specify a policy file, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorConfigurationDoesNotSpecifyPolicyFile() throws ConfigurationErrorException {
        setUpConfiguration();
        
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(false);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(null);

        new PolicyInstantiator(classFactory, permissionInstantiator);
    }
    
    // test case: When creating a new PolicyInstantiator instance and the configuration
    // does not specify a policy class name, it must throw a ConfigurationErrorException.
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorConfigurationDoesNotSpecifyPolicyClass() throws ConfigurationErrorException {
        setUpConfiguration();
        
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(false);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(null);

        new PolicyInstantiator(classFactory, permissionInstantiator);
    }
    
    // test case: When calling the method getRolePolicyInstance passing a policy String, it must create a new
    // policy instance using the ClassFactory createPluginInstance method and passing the policy String as argument.
    @Test
    public void testGetRolePolicyInstance() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpConfiguration();

        Mockito.when(classFactory.createPluginInstance(POLICY_CLASS, permissionInstantiator, POLICY_STRING,
                ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE)).thenReturn(policy);
        PolicyInstantiator policyInstantiator = new PolicyInstantiator(classFactory, permissionInstantiator);
        
        
        RolePolicy<FsOperation> returnedPolicy = policyInstantiator.getRolePolicyInstance(POLICY_STRING);
        
        
        assertEquals(policy, returnedPolicy);
        Mockito.verify(classFactory).createPluginInstance(POLICY_CLASS, permissionInstantiator, POLICY_STRING,
                ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE);
    }
    
    // test case: When calling the method getRolePolicyInstanceFromFile passing a policy file path, it must create a new
    // policy instance using the ClassFactory createPluginInstance method and passing the policy file as argument.
    @Test
    public void testGetRolePolicyInstanceFromFile() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpConfiguration();
        
        Mockito.when(classFactory.createPluginInstance(POLICY_CLASS, permissionInstantiator, 
                new File(HOME_DIR_PATH + POLICY_FILE), ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE)).thenReturn(policy);
        PolicyInstantiator policyInstantiator = new PolicyInstantiator(classFactory, permissionInstantiator);
        
        
        RolePolicy<FsOperation> returnedPolicy = policyInstantiator.getRolePolicyInstanceFromFile(HOME_DIR_PATH + POLICY_FILE);
        
        
        assertEquals(policy, returnedPolicy);
        Mockito.verify(classFactory).createPluginInstance(POLICY_CLASS, permissionInstantiator, new File(HOME_DIR_PATH + POLICY_FILE),
                ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE);
    }
    
    private void setUpConfiguration() {
        properties = Mockito.mock(Properties.class);
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(true);
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(true);
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(true);
        
        propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperties()).thenReturn(properties);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(ADMIN_ROLE);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(POLICY_FILE);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(POLICY_CLASS);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        PowerMockito.mockStatic(HomeDir.class);
        BDDMockito.given(HomeDir.getPath()).willReturn(HOME_DIR_PATH);
        
        permissionInstantiator = Mockito.mock(PermissionInstantiator.class);
        
        policy = Mockito.mock(RolePolicy.class);
        classFactory = Mockito.mock(FsClassFactory.class);
    }
}
