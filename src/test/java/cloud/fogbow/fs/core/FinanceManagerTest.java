package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.FinancePluginInstantiator;
import cloud.fogbow.fs.core.util.FinancePlanFactory;
import cloud.fogbow.ras.core.models.RasOperation;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, FinancePluginInstantiator.class, 
	FsPublicKeysHolder.class, AuthenticationUtil.class})
public class FinanceManagerTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_ID_3 = "userId3";
	private static final String USER_ID_TO_ADD_1 = "userIdToAdd1";
	private static final String USER_ID_TO_ADD_2 = "userIdToAdd1";
	private static final String USER_NAME_1 = "userName1";
	private static final String USER_NAME_2 = "userName2";
	private static final String USER_NAME_3 = "userName3";
	private static final String PROVIDER_USER_1 = "providerUserId1";
	private static final String PROVIDER_USER_2 = "providerUserId2";
	private static final String PROVIDER_USER_3 = "providerUserId3";
	private static final String PROVIDER_USER_TO_ADD_1 = "providerUserToAdd1";
	private static final String PROVIDER_USER_TO_ADD_2 = "providerUserToAdd1";
	private static final String PROPERTY_NAME_1 = "propertyName1";
	private static final String PROPERTY_VALUE_1 = "propertyValue1";
	private static final String PROPERTY_NAME_2 = "propertyName2";
	private static final String PROPERTY_VALUE_2 = "propertyValue2";
	private static final String PROPERTY_NAME_3 = "propertyName3";
	private static final String PROPERTY_VALUE_3 = "propertyValue3";
	private static final String USER_1_TOKEN = "user1Token";
	private static final String USER_2_TOKEN = "user2Token";
	private static final String USER_3_TOKEN = "user3Token";
	private static final String PLUGIN_1_NAME = "plugin1";
	private static final String PLUGIN_2_NAME = "plugin2";
	private static final String UNKNOWN_PLUGIN_NAME = "unknownplugin";
	private static final String DEFAULT_FINANCE_PLAN_NAME = "planName";
	private static final String DEFAULT_FINANCE_PLAN_FILE_PATH = "planPath";
    private static final String NEW_FINANCE_PLAN_NAME = "newPlan";
    private static final String PLAN_NAME = "plan";
	private List<FinancePlugin> financePlugins;
	private InMemoryFinanceObjectsHolder objectHolder;
	private FinancePlugin plugin1;
	private FinancePlugin plugin2;
	private AuthorizableUser user1;
	private AuthorizableUser user2;
	private AuthorizableUser user3;
	private SystemUser systemUser1;
	private SystemUser systemUser2;
	private SystemUser systemUser3;
	private RasOperation operation1;
	private RasOperation operation2;
	private RasOperation operation3;
	private FinancePlan financePlan;
	private FinancePlanFactory financePlanFactory;

	// test case: When calling the constructor, it must get
	// the names of the finance plugins from a PropertiesHolder
	// instance and call the FinancePluginInstantiator to 
	// instantiate the finance plugins.
	@Test
	public void testConstructor() throws FogbowException {
		setUpFinancePlugin();

		String pluginName1 = "plugin1";
		String pluginName2 = "plugin2";
		
		String pluginString = String.join(FinanceManager.FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR, 
				pluginName1, pluginName2);
		
		PowerMockito.mockStatic(PropertiesHolder.class);
		PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES)).thenReturn(pluginString);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(DEFAULT_FINANCE_PLAN_NAME);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_FILE_PATH)).thenReturn(DEFAULT_FINANCE_PLAN_FILE_PATH);
		BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

		PowerMockito.mockStatic(FinancePluginInstantiator.class);
		BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName1, objectHolder)).willReturn(plugin1);
		BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName2, objectHolder)).willReturn(plugin2);

		objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
		financePlan = Mockito.mock(FinancePlan.class);
		Mockito.when(objectHolder.getFinancePlan(DEFAULT_FINANCE_PLAN_NAME)).thenReturn(financePlan);
		
		
		new FinanceManager(objectHolder, new FinancePlanFactory());
		
		
		Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
		
		PowerMockito.verifyStatic(FinancePluginInstantiator.class);
		FinancePluginInstantiator.getFinancePlugin(pluginName1, objectHolder);
		PowerMockito.verifyStatic(FinancePluginInstantiator.class);
		FinancePluginInstantiator.getFinancePlugin(pluginName2, objectHolder);
	}
	
	// test case: When calling the constructor and the list
	// of finance plugins is empty, it must throw a ConfigurationErrorException.
	@Test(expected = ConfigurationErrorException.class)
	public void testConstructorThrowsExceptionIfNoFinancePluginIsGiven() throws ConfigurationErrorException, 
	InvalidParameterException, InternalServerErrorException {
		String pluginString = "";

		PowerMockito.mockStatic(PropertiesHolder.class);
		PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES)).thenReturn(pluginString);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(DEFAULT_FINANCE_PLAN_NAME);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_FILE_PATH)).thenReturn(DEFAULT_FINANCE_PLAN_FILE_PATH);
		BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
		
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        financePlan = Mockito.mock(FinancePlan.class);
        Mockito.when(objectHolder.getFinancePlan(DEFAULT_FINANCE_PLAN_NAME)).thenReturn(financePlan);
		
		new FinanceManager(objectHolder, new FinancePlanFactory());
	}
	
	// test case: When calling the constructor and the default finance plan does 
	// not exist in the database, it must call the FinancePlanFactory to create 
	// the default plan and call the DatabaseManager to save the plan.
	@Test
	public void testContructorDefaultFinancePlanDoesNotExist() throws FogbowException {
        setUpFinancePlugin();

        String pluginName1 = "plugin1";
        String pluginName2 = "plugin2";
        
        String pluginString = String.join(FinanceManager.FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR, 
                pluginName1, pluginName2);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES)).thenReturn(pluginString);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(DEFAULT_FINANCE_PLAN_NAME);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_FILE_PATH)).thenReturn(DEFAULT_FINANCE_PLAN_FILE_PATH);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        financePlan = Mockito.mock(FinancePlan.class);
        Mockito.when(objectHolder.getFinancePlan(DEFAULT_FINANCE_PLAN_NAME)).thenThrow(new InvalidParameterException());

        PowerMockito.mockStatic(FinancePluginInstantiator.class);
        BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName1, objectHolder)).willReturn(plugin1);
        BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName2, objectHolder)).willReturn(plugin2);
        
        FinancePlanFactory financePlanFactory = Mockito.mock(FinancePlanFactory.class);
        Mockito.when(financePlanFactory.createFinancePlan(DEFAULT_FINANCE_PLAN_NAME, DEFAULT_FINANCE_PLAN_FILE_PATH)).thenReturn(financePlan);
        
        
        new FinanceManager(objectHolder, financePlanFactory);
        
        
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
        Mockito.verify(financePlanFactory).createFinancePlan(DEFAULT_FINANCE_PLAN_NAME, DEFAULT_FINANCE_PLAN_FILE_PATH);
        Mockito.verify(objectHolder).registerFinancePlan(financePlan);
        PowerMockito.verifyStatic(FinancePluginInstantiator.class);
        FinancePluginInstantiator.getFinancePlugin(pluginName1, objectHolder);
        PowerMockito.verifyStatic(FinancePluginInstantiator.class);
        FinancePluginInstantiator.getFinancePlugin(pluginName2, objectHolder);
	}
	
	// test case: When calling the constructor passing an empty list
	// of finance plugins, it must throw a ConfigurationErrorException.
	@Test(expected = ConfigurationErrorException.class)
	public void testConstructorChecksPluginsListIsNotEmpty() throws ConfigurationErrorException {
		financePlugins = new ArrayList<FinancePlugin>();
		
		objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
		
		new FinanceManager(financePlugins, objectHolder, financePlanFactory);
	}
	
	// test case: When calling the isAuthorized method passing an AuthorizableUser,
	// it must check which finance plugin manages the user and call the isAuthorized 
	// method of the plugin.
	@Test
	public void testIsAuthorized() throws FogbowException {
		setUpFinancePlugin();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		
		assertTrue(financeManager.isAuthorized(user1));
		assertTrue(financeManager.isAuthorized(user2));
		assertFalse(financeManager.isAuthorized(user3));
	}

	// test case: When calling the isAuthorized method passing an AuthorizableUser which
	// is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testIsAuthorizedUserIsNotManaged() throws FogbowException {
		setUpFinancePluginUnmanagedUser();
		setUpAuthentication();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		financeManager.isAuthorized(user1);
	}
	
	// test case: When calling the getFinanceStateProperty method passing an AuthorizableUser, 
	// it must check which finance plugin manages the user and call the getUserFinanceState
	// method of the plugin.
	@Test
	public void testGetFinanceStateProperty() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		
		assertEquals(PROPERTY_VALUE_1, financeManager.getFinanceStateProperty(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1));
		assertEquals(PROPERTY_VALUE_2, financeManager.getFinanceStateProperty(USER_ID_2, PROVIDER_USER_2, PROPERTY_NAME_2));
		assertEquals(PROPERTY_VALUE_3, financeManager.getFinanceStateProperty(USER_ID_3, PROVIDER_USER_3, PROPERTY_NAME_3));
	}
	
	// test case: When calling the getFinanceStateProperty method passing an AuthorizableUser
	// which is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testGetFinanceStatePropertyUserIsNotManaged() throws FogbowException {
		setUpFinancePluginUnmanagedUser();

		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		financeManager.getFinanceStateProperty(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1);
	}
	
	// test case: When calling the addUser method, it must add the 
	// user using the correct FinancePlugin.
	@Test
	public void testAddUser() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		Map<String, String> financeOptions = new HashMap<String, String>();
		User user1 = new User(USER_ID_TO_ADD_1, PROVIDER_USER_TO_ADD_1, PLUGIN_1_NAME, financeOptions);
		User user2 = new User(USER_ID_TO_ADD_2, PROVIDER_USER_TO_ADD_2, PLUGIN_2_NAME, financeOptions);
		
		financeManager.addUser(user1);
		financeManager.addUser(user2);

		Mockito.verify(plugin1, Mockito.times(1)).addUser(USER_ID_TO_ADD_1, PROVIDER_USER_TO_ADD_1, financeOptions);
		Mockito.verify(plugin2, Mockito.times(1)).addUser(USER_ID_TO_ADD_2, PROVIDER_USER_TO_ADD_2, financeOptions);
	}
	
	// test case: When calling the addUser method and the FinanceManager
	// does not know the finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testAddUserUnknownPlugin() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		Map<String, String> financeOptions = new HashMap<String, String>();
		User user1 = new User(USER_ID_TO_ADD_1, PROVIDER_USER_TO_ADD_1, UNKNOWN_PLUGIN_NAME, financeOptions);
		
		financeManager.addUser(user1);
	}
	
	// test case: When calling the removeUser method, it must remove 
	// the user using the correct FinancePlugin.
	@Test
	public void testRemoveUser() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		
		financeManager.removeUser(USER_ID_1, PROVIDER_USER_1);
		financeManager.removeUser(USER_ID_2, PROVIDER_USER_2);
		financeManager.removeUser(USER_ID_3, PROVIDER_USER_3);
		
		Mockito.verify(plugin1, Mockito.times(1)).removeUser(USER_ID_1, PROVIDER_USER_1);
		Mockito.verify(plugin1, Mockito.times(1)).removeUser(USER_ID_3, PROVIDER_USER_3);
		Mockito.verify(plugin2, Mockito.times(1)).removeUser(USER_ID_2, PROVIDER_USER_2);
	}
	
	// test case: When calling the removeUser method and the user
	// to be removed is not managed by any finance plugin, it must 
	// throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testRemoveUserUnmanagedUser() throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
		setUpFinancePluginUnmanagedUser();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		
		financeManager.removeUser(USER_ID_1, PROVIDER_USER_1);
	}
	
	// test case: When calling the changeOptions method, it must
	// change the user's options using the correct FinancePlugin. 
	@Test
	public void testChangeOptions() throws FogbowException {
		setUpFinancePlugin();
		Map<String, String> newFinanceOptions = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		financeManager.changeOptions(USER_ID_1, PROVIDER_USER_1, newFinanceOptions);
		
		Mockito.verify(plugin1, Mockito.times(1)).changeOptions(USER_ID_1, PROVIDER_USER_1, newFinanceOptions);
	}
	
	// test case: When calling the changeOptions method and the user
	// is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testChangeOptionsUnmanagedUser() throws ConfigurationErrorException, InvalidParameterException, InternalServerErrorException {
		setUpFinancePluginUnmanagedUser();
		Map<String, String> newFinanceOptions = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		
		financeManager.changeOptions(USER_ID_1, PROVIDER_USER_1, newFinanceOptions);
	}
	
	// test case: When calling the updateFinanceState method, it must
	// change the user's finance state using the correct FinancePlugin.
	@Test
	public void testUpdateFinanceState() throws FogbowException {
		setUpFinancePlugin();
		Map<String, String> newFinanceState = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		financeManager.updateFinanceState(USER_ID_1, PROVIDER_USER_1, newFinanceState);
		
		Mockito.verify(plugin1, Mockito.times(1)).updateFinanceState(USER_ID_1, PROVIDER_USER_1, newFinanceState);
	}
	
	// test case: When calling the updateFinanceState method and the user
	// is not managed by any finance plugin, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testUpdateFinanceStateUnmanagedUser() throws InvalidParameterException, 
	ConfigurationErrorException, InternalServerErrorException {
		setUpFinancePluginUnmanagedUser();
		
		Map<String, String> newFinanceState = new HashMap<String, String>();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		financeManager.updateFinanceState(USER_ID_1, PROVIDER_USER_1, newFinanceState);
	}
	
	// test case: When calling the startPlugins method, it must call the startThreads
	// method of all the known finance plugins.
	@Test
	public void testStartThreads() throws FogbowException {
		setUpFinancePlugin();
		
		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
		
		financeManager.startPlugins();
		
		Mockito.verify(plugin1, Mockito.times(1)).startThreads();
		Mockito.verify(plugin2, Mockito.times(1)).startThreads();
	}

	// test case: When calling the stopPlugins method, it must call the stopThreads
	// method of all the known finance plugins.
	@Test
	public void testStopThreads() throws FogbowException {
		setUpFinancePlugin();

		FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);

		financeManager.stopPlugins();

		Mockito.verify(plugin1, Mockito.times(1)).stopThreads();
		Mockito.verify(plugin2, Mockito.times(1)).stopThreads();
	}
	
	// test case: When calling the reset plugins method, it must get
    // the names of the finance plugins from a PropertiesHolder
    // instance and call the FinancePluginInstantiator to 
    // instantiate the finance plugins.
	@Test
	public void testResetPlugins() throws FogbowException {
	    setUpFinancePlugin();
	    
	    FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
	    
	    String pluginName3 = "plugin3";
        String pluginName4 = "plugin4";
        
        String pluginString = String.join(FinanceManager.FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR, 
                pluginName3, pluginName4);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES)).thenReturn(pluginString);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
	    
        PowerMockito.mockStatic(FinancePluginInstantiator.class);
        BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName3, objectHolder)).willReturn(plugin1);
        BDDMockito.given(FinancePluginInstantiator.getFinancePlugin(pluginName4, objectHolder)).willReturn(plugin2);
	    
	    
	    financeManager.resetPlugins();
	    
	    
	    Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
        
        PowerMockito.verifyStatic(FinancePluginInstantiator.class);
        FinancePluginInstantiator.getFinancePlugin(pluginName3, objectHolder);
        PowerMockito.verifyStatic(FinancePluginInstantiator.class);
        FinancePluginInstantiator.getFinancePlugin(pluginName4, objectHolder);
	}
	
	// test case: When calling the createFinancePlan method, it must call the 
	// FinancePlanFactory to create a new FinancePlan and call the saveFinancePlan
	// method of the DatabaseManager.
	@Test
	public void testCreateFinancePlan() throws FogbowException {
	    setUpFinancePlugin();
	    
	    Map<String, String> planInfo = new HashMap<String, String>();
	    FinancePlan plan = Mockito.mock(FinancePlan.class);
	    
	    this.financePlanFactory = Mockito.mock(FinancePlanFactory.class);
	    Mockito.when(this.financePlanFactory.createFinancePlan(NEW_FINANCE_PLAN_NAME, planInfo)).thenReturn(plan);
	    
	    objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
	    FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
	    
	    
        financeManager.createFinancePlan(NEW_FINANCE_PLAN_NAME, planInfo);
        
        
        Mockito.verify(financePlanFactory).createFinancePlan(NEW_FINANCE_PLAN_NAME, planInfo);
        Mockito.verify(objectHolder).registerFinancePlan(plan);
	}
	
	// test case: When calling the getFinancePlan method, it must call the
	// DatabaseManager to get the correct FinancePlan object and then
	// return the same map obtained by calling the FinancePlan.getRulesAsMap method.
	@Test
	public void testGetFinancePlan() throws FogbowException {
        setUpFinancePlugin();
        
        this.financePlanFactory = Mockito.mock(FinancePlanFactory.class);

        Map<String, String> planInfo = new HashMap<String, String>();
        FinancePlan plan = Mockito.mock(FinancePlan.class);
        Mockito.when(plan.getRulesAsMap()).thenReturn(planInfo);
        
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(this.objectHolder.getFinancePlan(NEW_FINANCE_PLAN_NAME)).thenReturn(plan);
        
        FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
        
        assertEquals(planInfo, financeManager.getFinancePlan(NEW_FINANCE_PLAN_NAME));
	}
	
	// test case: When calling the updateFinancePlan method, it must call the 
	// InMemoryFinanceObjectsHolder to update the finance plan.
    @Test
    public void testUpdateFinancePlan() throws FogbowException {
        setUpFinancePlugin();
        
        Map<String, String> newPlanInfo = new HashMap<String, String>();
        FinancePlan plan = Mockito.mock(FinancePlan.class);
        
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        Mockito.when(this.objectHolder.getFinancePlan(PLAN_NAME)).thenReturn(plan);
        
        FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
        
        
        financeManager.updateFinancePlan(PLAN_NAME, newPlanInfo);
        
        Mockito.verify(objectHolder).updateFinancePlan(PLAN_NAME, newPlanInfo);
    }
    
    // test case: When calling the removeFinancePlan method, it must call
    // the InMemoryFinanceObjectsHolder to remove the finance plan.
    @Test
    public void testRemoveFinancePlan() throws FogbowException {
        setUpFinancePlugin();
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(DEFAULT_FINANCE_PLAN_NAME);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        objectHolder = Mockito.mock(InMemoryFinanceObjectsHolder.class);
        
        
        FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
        
        
        financeManager.removeFinancePlan(PLAN_NAME);
        
        Mockito.verify(objectHolder).removeFinancePlan(PLAN_NAME);
    }
    
    // test case: When calling the removeFinancePlan method passing 
    // the default plan name as argument, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testCannotRemoveDefaultFinancePlan() throws FogbowException {
        setUpFinancePlugin();
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(
                ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(DEFAULT_FINANCE_PLAN_NAME);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        
        FinanceManager financeManager = new FinanceManager(financePlugins, objectHolder, financePlanFactory);
        
        
        financeManager.removeFinancePlan(DEFAULT_FINANCE_PLAN_NAME);
    }
	
	private void setUpFinancePlugin() throws FogbowException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		systemUser2 = new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2);
		systemUser3 = new SystemUser(USER_ID_3, USER_NAME_3, PROVIDER_USER_3);
		
		operation1 = Mockito.mock(RasOperation.class);
		operation2 = Mockito.mock(RasOperation.class);
		operation3 = Mockito.mock(RasOperation.class);

		financePlugins = new ArrayList<FinancePlugin>();
		
		this.plugin1 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin1.managesUser(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
		Mockito.when(plugin1.managesUser(USER_ID_2, PROVIDER_USER_2)).thenReturn(false);
		Mockito.when(plugin1.managesUser(USER_ID_3, PROVIDER_USER_3)).thenReturn(true);
		Mockito.when(plugin1.getName()).thenReturn(PLUGIN_1_NAME);
		Mockito.when(plugin1.isAuthorized(systemUser1, operation1)).thenReturn(true);
		Mockito.when(plugin1.isAuthorized(systemUser3, operation3)).thenReturn(false);
		Mockito.when(plugin1.getUserFinanceState(USER_ID_1, PROVIDER_USER_1, PROPERTY_NAME_1)).thenReturn(PROPERTY_VALUE_1);
		Mockito.when(plugin1.getUserFinanceState(USER_ID_3, PROVIDER_USER_3, PROPERTY_NAME_3)).thenReturn(PROPERTY_VALUE_3);
		
		this.plugin2 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin2.managesUser(USER_ID_2, PROVIDER_USER_2)).thenReturn(true);
		Mockito.when(plugin2.getName()).thenReturn(PLUGIN_2_NAME);
		Mockito.when(plugin2.isAuthorized(systemUser2, operation2)).thenReturn(true);
		Mockito.when(plugin2.getUserFinanceState(USER_ID_2, PROVIDER_USER_2, PROPERTY_NAME_2)).thenReturn(PROPERTY_VALUE_2);
		
		financePlugins.add(plugin1);
		financePlugins.add(plugin2);
		
		this.user1 = Mockito.mock(AuthorizableUser.class);
		this.user2 = Mockito.mock(AuthorizableUser.class);
		this.user3 = Mockito.mock(AuthorizableUser.class);
		
		Mockito.when(this.user1.getUserToken()).thenReturn(USER_1_TOKEN);
		Mockito.when(this.user2.getUserToken()).thenReturn(USER_2_TOKEN);
		Mockito.when(this.user3.getUserToken()).thenReturn(USER_3_TOKEN);

		Mockito.when(this.user1.getOperation()).thenReturn(operation1);
		Mockito.when(this.user2.getOperation()).thenReturn(operation2);
		Mockito.when(this.user3.getOperation()).thenReturn(operation3);
	}
	
	private void setUpFinancePluginUnmanagedUser() throws InvalidParameterException, InternalServerErrorException {
		systemUser1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
		operation1 = Mockito.mock(RasOperation.class);
		
		financePlugins = new ArrayList<FinancePlugin>();
		
		this.plugin1 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin1.managesUser(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		this.plugin2 = Mockito.mock(FinancePlugin.class);
		Mockito.when(plugin2.managesUser(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
		
		financePlugins.add(plugin1);
		financePlugins.add(plugin2);
		
		this.user1 = Mockito.mock(AuthorizableUser.class);
		
		Mockito.when(this.user1.getUserToken()).thenReturn(USER_1_TOKEN);
		Mockito.when(this.user1.getOperation()).thenReturn(operation1);
	}
	
	private void setUpAuthentication() throws FogbowException {
		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		FsPublicKeysHolder fsPublicKeysHolder = Mockito.mock(FsPublicKeysHolder.class);
		RSAPublicKey rasPublicKey = Mockito.mock(RSAPublicKey.class);
		Mockito.when(fsPublicKeysHolder.getRasPublicKey()).thenReturn(rasPublicKey);
		
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(fsPublicKeysHolder);
		
		PowerMockito.mockStatic(AuthenticationUtil.class);
		BDDMockito.given(AuthenticationUtil.authenticate(rasPublicKey, USER_1_TOKEN)).willReturn(systemUser1);
		BDDMockito.given(AuthenticationUtil.authenticate(rasPublicKey, USER_2_TOKEN)).willReturn(systemUser2);
		BDDMockito.given(AuthenticationUtil.authenticate(rasPublicKey, USER_3_TOKEN)).willReturn(systemUser3);
	}
}
