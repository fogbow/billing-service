package cloud.fogbow.fs.core.plugins.plan.postpaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.core.InMemoryUsersHolder;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.InvoiceState;
import cloud.fogbow.fs.core.plugins.plan.postpaid.PostPaidPlanPlugin.PostPaidPluginOptionsLoader;
import cloud.fogbow.fs.core.util.AccountingServiceClient;
import cloud.fogbow.fs.core.util.FinancePlanFactory;
import cloud.fogbow.fs.core.util.JsonUtils;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.RasClient;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class PostPaidPlanPluginTest {
    private static final String INVOICE_WAIT_TIME = "30000";
    private static final String FINANCE_PLAN_RULES_FILE_PATH = "rules_path";
    private static final String USER_BILLING_INTERVAL = "60000";
    private static final String USER_ID_1 = "userId1";
    private static final String USER_ID_2 = "userId2";
    private static final String USER_NAME_1 = "userName1";
    private static final String USER_NAME_2 = "userName2";
    private static final String PROVIDER_USER_1 = "providerUser1";
    private static final String PROVIDER_USER_2 = "providerUser2";
    private static final String USER_NOT_MANAGED = "userNotManaged";
    private static final String PROVIDER_USER_NOT_MANAGED = "providerUserNotManaged";
    private static final String USER_BILLING_INTERVAL_1 = "10";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
    private static final String PLAN_NAME = "planName";
    private static final String RULES_JSON = "rulesjson";
    private static final String NEW_RULES_JSON = "newRulesJson";
    private static final String FINANCE_PLAN_FILE_PATH = null;
    private AccountingServiceClient accountingServiceClient;
    private RasClient rasClient;
    private InvoiceManager paymentManager;
    private FinancePlanFactory planFactory;
    private long userBillingInterval = 10L;
    private long newUserBillingInterval = 20L;
    private long invoiceWaitTime = 1L;
    private long newInvoiceWaitTime = 2L;
    private Map<String, String> financeState;
    private InMemoryUsersHolder objectHolder;
    private JsonUtils jsonUtils;
    private FinancePlan plan;
    private Map<String, String> rulesMap = new HashMap<String, String>();
    private Map<String, String> newRulesMap = new HashMap<String, String>();

    // test case: When calling the managesUser method, it must
    // get the user from the objects holder and check if the user
    // is managed by the plugin.
    @Test
    public void testManagesUser() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        FinanceUser financeUser1 = new FinanceUser();
        financeUser1.setUserId(USER_ID_1, PROVIDER_USER_1);
        financeUser1.setFinancePluginName(PLAN_NAME);
        
        FinanceUser financeUser2 = new FinanceUser();
        financeUser1.setUserId(USER_ID_2, PROVIDER_USER_2);
        financeUser2.setFinancePluginName(PLAN_NAME);
        
        FinanceUser financeUser3 = new FinanceUser();
        financeUser1.setUserId(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED);
        financeUser3.setFinancePluginName("otherplan");
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(financeUser1);
        Mockito.when(objectHolder.getUserById(USER_ID_2, PROVIDER_USER_2)).thenReturn(financeUser2);
        Mockito.when(objectHolder.getUserById(USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)).thenReturn(financeUser3);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                jsonUtils, plan, financeOptions);
        
        assertTrue(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1)));
        assertTrue(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_USER_2)));
        assertFalse(postPaidFinancePlugin.isRegisteredUser(new SystemUser(USER_NOT_MANAGED, USER_NOT_MANAGED, PROVIDER_USER_NOT_MANAGED)));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedCreateOperationUserStateIsGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(true);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                jsonUtils, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for an
    // operation other than creation and the user financial state is not good, 
    // the method must return true.
    @Test
    public void testIsAuthorizedNonCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertTrue(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the isAuthorized method for a
    // creation operation and the user financial state is not good, 
    // the method must return false.
    @Test
    public void testIsAuthorizedCreationOperationUserStateIsNotGoodFinancially() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        this.paymentManager = Mockito.mock(InvoiceManager.class);
        Mockito.when(this.paymentManager.hasPaid(USER_ID_1, PROVIDER_USER_1)).thenReturn(false);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        SystemUser user = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1);
        RasOperation operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, USER_ID_1, PROVIDER_USER_1);
        
        assertFalse(postPaidFinancePlugin.isAuthorized(user, operation));
    }
    
    // test case: When calling the addUser method, it must call the DatabaseManager
    // to register the user using given parameters.
    @Test
    public void testAddUser() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        
        postPaidFinancePlugin.registerUser(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1));
        
        
        Mockito.verify(this.objectHolder).registerUser(USER_ID_1, PROVIDER_USER_1, PLAN_NAME);
    }

    // test case: When calling the changeOptions method, it must call the DatabaseManager
    // to change the options for the given user.
    @Test
    public void testChangeOptions() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, USER_BILLING_INTERVAL_1);
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the changeOptions method and the finance options map 
    // does not contain some required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testChangeOptionsMissingOption() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        
        financeOptions = new HashMap<String, String>();

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the changeOptions method and the finance options map 
    // contains an invalid value for a required financial option, it must throw an
    // InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testChangeOptionsInvalidOption() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, "invalidoption");
        
        objectHolder = Mockito.mock(InMemoryUsersHolder.class);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        postPaidFinancePlugin.setOptions(financeOptions);
    }
    
    // test case: When calling the updateFinanceState method, it must get the correct invoices
    // from the database, change the invoices states and save the invoices.
    @Test
    public void testUpdateFinanceState() throws InvalidParameterException, InternalServerErrorException {
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(userBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(invoiceWaitTime));
        
        Invoice invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getInvoiceId()).thenReturn(INVOICE_ID_1);
        
        Invoice invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getInvoiceId()).thenReturn(INVOICE_ID_2);
        
        ArrayList<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(invoice1);
        invoices.add(invoice2);
        
        FinanceUser user = Mockito.mock(FinanceUser.class);
        
        this.objectHolder = Mockito.mock(InMemoryUsersHolder.class);
        Mockito.when(objectHolder.getUserById(USER_ID_1, PROVIDER_USER_1)).thenReturn(user);
        Mockito.when(user.getInvoices()).thenReturn(invoices);
        
        financeState = new HashMap<String, String>();
        financeState.put(INVOICE_ID_1, InvoiceState.PAID.getValue());
        financeState.put(INVOICE_ID_2, InvoiceState.DEFAULTING.getValue());

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, invoiceWaitTime, objectHolder, 
                accountingServiceClient, rasClient, paymentManager, planFactory, jsonUtils, plan, financeOptions);
        
        
        postPaidFinancePlugin.updateUserFinanceState(new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_USER_1), financeState);
        
        
        Mockito.verify(invoice1).setState(InvoiceState.PAID);
        Mockito.verify(invoice2).setState(InvoiceState.DEFAULTING);
    }
    
    // TODO documentation
    @Test
    public void testSetOptions() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(rulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);
        
        Map<String, String> optionsBefore = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(userBillingInterval), optionsBefore.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(invoiceWaitTime), optionsBefore.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(RULES_JSON, optionsBefore.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        
        postPaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        // rules are not updated
        assertEquals(RULES_JSON, optionsAfter.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
    }
    
    // TODO documentation
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNotNull() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(rulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);
        Mockito.when(this.jsonUtils.fromJson(NEW_RULES_JSON, Map.class)).thenReturn(newRulesMap);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);
        
        Map<String, String> optionsBefore = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(userBillingInterval), optionsBefore.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(invoiceWaitTime), optionsBefore.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(RULES_JSON, optionsBefore.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        
        postPaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        
        Mockito.verify(this.plan).update(newRulesMap);
    }
    
    // TODO documentation
    @Test
    public void testSetOptionsWithPlanRulesPlanIsNull() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(newRulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(newRulesMap)).thenReturn(NEW_RULES_JSON);
        Mockito.when(this.jsonUtils.fromJson(NEW_RULES_JSON, Map.class)).thenReturn(newRulesMap);
        
        this.planFactory = Mockito.mock(FinancePlanFactory.class);
        Mockito.when(this.planFactory.createFinancePlan(PLAN_NAME, newRulesMap)).thenReturn(plan);
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, null);
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES, NEW_RULES_JSON);
        
        postPaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(NEW_RULES_JSON, optionsAfter.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        Mockito.verify(this.planFactory).createFinancePlan(PLAN_NAME, newRulesMap);
    }
    
    // TODO documentation
    @Test
    public void testSetOptionsWithPlanRuleFromFile() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(newRulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(newRulesMap)).thenReturn(NEW_RULES_JSON);
        
        this.planFactory = Mockito.mock(FinancePlanFactory.class);
        Mockito.when(this.planFactory.createFinancePlan(PLAN_NAME, FINANCE_PLAN_FILE_PATH)).thenReturn(plan);
        
        
        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);
        
        // new options
        Map<String, String> financeOptions = new HashMap<String, String>();
        financeOptions.put(PaymentRunner.USER_BILLING_INTERVAL, String.valueOf(newUserBillingInterval));
        financeOptions.put(PostPaidPlanPlugin.INVOICE_WAIT_TIME, String.valueOf(newInvoiceWaitTime));
        financeOptions.put(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH, FINANCE_PLAN_FILE_PATH);
        
        postPaidFinancePlugin.setOptions(financeOptions);
        
        
        Map<String, String> optionsAfter = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(newUserBillingInterval), optionsAfter.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(newInvoiceWaitTime), optionsAfter.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(NEW_RULES_JSON, optionsAfter.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
        
        Mockito.verify(this.planFactory).createFinancePlan(PLAN_NAME, FINANCE_PLAN_FILE_PATH);
    }
    
    // TODO documentation
    @Test
    public void testGetOptions() throws InvalidParameterException, InternalServerErrorException {
        this.plan = Mockito.mock(FinancePlan.class);
        Mockito.when(this.plan.getRulesAsMap()).thenReturn(rulesMap);
        
        this.jsonUtils = Mockito.mock(JsonUtils.class);
        Mockito.when(this.jsonUtils.toJson(rulesMap)).thenReturn(RULES_JSON);

        PostPaidPlanPlugin postPaidFinancePlugin = new PostPaidPlanPlugin(PLAN_NAME, userBillingInterval, 
                invoiceWaitTime, objectHolder, accountingServiceClient, rasClient, paymentManager, planFactory, 
                this.jsonUtils, this.plan);

        Map<String, String> options = postPaidFinancePlugin.getOptions();
        
        assertEquals(String.valueOf(userBillingInterval), options.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(String.valueOf(invoiceWaitTime), options.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
        assertEquals(RULES_JSON, options.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES));
    }
    
    // TODO documentation
    @Test
    public void testPostPaidPluginOptionsLoaderValidOptions() throws ConfigurationErrorException {
        setUpOptions(USER_BILLING_INTERVAL, FINANCE_PLAN_RULES_FILE_PATH, INVOICE_WAIT_TIME);
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        Map<String, String> options = loader.load();
        
        assertEquals(USER_BILLING_INTERVAL, options.get(PaymentRunner.USER_BILLING_INTERVAL));
        assertEquals(FINANCE_PLAN_RULES_FILE_PATH, options.get(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH));
        assertEquals(INVOICE_WAIT_TIME, options.get(PostPaidPlanPlugin.INVOICE_WAIT_TIME));
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingUserBillingInterval() throws ConfigurationErrorException {
        setUpOptions(null, FINANCE_PLAN_RULES_FILE_PATH, INVOICE_WAIT_TIME);
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingFinancePlanRulesFilePath() throws ConfigurationErrorException {
        setUpOptions(USER_BILLING_INTERVAL, null, INVOICE_WAIT_TIME);
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testPostPaidPluginOptionsLoaderMissingInvoiceWaitTime() throws ConfigurationErrorException {
        setUpOptions(USER_BILLING_INTERVAL, FINANCE_PLAN_RULES_FILE_PATH, null);
        
        PostPaidPluginOptionsLoader loader = new PostPaidPluginOptionsLoader();
        
        loader.load();
    }
    
    private void setUpOptions(String userBillingInterval, String financePlanRulesPath, String invoiceWaitTime) {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        
        Mockito.when(propertiesHolder.getProperty(PaymentRunner.USER_BILLING_INTERVAL)).thenReturn(userBillingInterval);
        Mockito.when(propertiesHolder.getProperty(PostPaidPlanPlugin.FINANCE_PLAN_RULES_FILE_PATH)).thenReturn(financePlanRulesPath);
        Mockito.when(propertiesHolder.getProperty(PostPaidPlanPlugin.INVOICE_WAIT_TIME)).thenReturn(invoiceWaitTime);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }
}
