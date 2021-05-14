package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.models.UserCredits;
import cloud.fogbow.fs.core.plugins.payment.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class InMemoryFinanceObjectsHolderTest {

    private static final String PLUGIN_NAME_1 = "plugin1";
    private static final String PLUGIN_NAME_2 = "plugin2";
    private static final String USER_ID_TO_ADD = "userIdToAdd";
    private static final String PROVIDER_ID_TO_ADD = "providerIdToAdd";
    private static final String USER_ID_1 = "userId1";
    private static final String PROVIDER_ID_1 = "providerId1";
    private static final String USER_ID_2 = "userId2";
    private static final String PROVIDER_ID_2 = "providerId2";
    private static final String INVOICE_ID_1 = "invoiceId1";
    private static final String INVOICE_ID_2 = "invoiceId2";
    private static final String INVOICE_ID_3 = "invoiceId3";
    private static final String PLAN_NAME_1 = "plan1";
    private static final String PLAN_NAME_2 = "plan2";
    private static final String NEW_PLAN_NAME_1 = "newplan1";
    
    private DatabaseManager databaseManager;
    private MultiConsumerSynchronizedListFactory listFactory;
    private UserCreditsFactory userCreditsFactory;
    private FinanceUser user1;
    private FinanceUser user2;
    private MultiConsumerSynchronizedList<Object> userSynchronizedList1;
    private MultiConsumerSynchronizedList<Object> userSynchronizedList2;
    private MultiConsumerSynchronizedList<Object> planSynchronizedList;
    private List<FinanceUser> usersList;
    private List<Invoice> invoicesList;
    private Invoice invoice1;
    private Invoice invoice2;
    private Invoice invoice3;
    private List<UserCredits> creditsList;
    private UserCredits userCredits1;
    private UserCredits userCredits2;
    private List<FinancePlan> plansList;
    private FinancePlan plan1;
    private FinancePlan plan2;
    private Map<String, String> rulesPlan1;
    private InMemoryFinanceObjectsHolder objectHolder;
    
    @Before
    public void setUp() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpUsers();
        setUpInvoices();
        setUpCredits();
        setUpPlans();
        setUpDatabase();
        setUpLists();
    }
    
    // test case: When creating a new InMemoryFinanceObjectsHolder instance, the constructor
    // must acquire the data from FinanceUsers and FinancePlans using the DatabaseManager and
    // prepare its internal data holding lists properly.
    @Test
    public void testConstructorSetsUpDataStructuresCorrectly() throws InternalServerErrorException, InvalidParameterException {
        new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        Mockito.verify(databaseManager).getRegisteredUsers();
        Mockito.verify(databaseManager).getRegisteredFinancePlans();
        
        Mockito.verify(listFactory, Mockito.times(3)).getList();
        
        Mockito.verify(userSynchronizedList1).addItem(user1);
        Mockito.verify(userSynchronizedList2).addItem(user2);
        Mockito.verify(planSynchronizedList).addItem(plan1);
        Mockito.verify(planSynchronizedList).addItem(plan2);
    }

    // test case: When calling the method registerUser, it must add a new FinanceUser instance
    // to the list of FinanceUsers and persist the new user by calling the DatabaseManager.
    @Test
    public void testRegisterUser() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.registerUser(USER_ID_TO_ADD, PROVIDER_ID_TO_ADD, PLUGIN_NAME_1, new HashMap<String, String>());
        
        Mockito.verify(userSynchronizedList1, Mockito.times(2)).addItem(Mockito.any(FinanceUser.class));
        Mockito.verify(databaseManager).saveUser(Mockito.any(FinanceUser.class));
    }

    // test case: When calling the method registerUser passing UserId and ProviderId
    // used by an already registered user, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void cannotRegisterUserWithAlreadyUsedUserIdAndProvider() throws InternalServerErrorException, 
            InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);

        objectHolder.registerUser(USER_ID_1, PROVIDER_ID_1, PLUGIN_NAME_1, rulesPlan1);
    }
    
    // test case: When calling the method removeUser, it must remove the given user from
    // the list of FinanceUsers and delete the user from the database using the DatabaseManager.
    @Test
    public void testRemoveUser() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.removeUser(USER_ID_1, PROVIDER_ID_1);
        
        Mockito.verify(userSynchronizedList1).removeItem(Mockito.any(FinanceUser.class));
        Mockito.verify(databaseManager).removeUser(USER_ID_1, PROVIDER_ID_1);
    }
    
    // test case: When calling the method removeUser passing unknown UserId and ProviderId, it 
    // must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveUnknownUser() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.removeUser("unknownuser", "unknownprovider");
    }
    
    // test case: When calling the method getUserById, it must iterate over the correct list of 
    // users and return the correct user.
    @Test
    public void testGetUserById() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        assertEquals(user1, objectHolder.getUserById(USER_ID_1, PROVIDER_ID_1));
        assertEquals(user2, objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2));
        
        Mockito.verify(userSynchronizedList1, Mockito.times(2)).stopIterating(Mockito.anyInt());
        Mockito.verify(userSynchronizedList2, Mockito.times(1)).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getUserById and a concurrent modification on the list
    // occurs while the method is iterating over it, it must restart the iteration and return 
    // the correct user.
    @Test
    public void testGetUserByIdListChanges() throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        Mockito.when(userSynchronizedList1.getNext(Mockito.anyInt())).
        thenReturn(user1).
        thenThrow(new ModifiedListException()).
        thenReturn(user1, user2);

        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        assertEquals(user2, objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2));
        
        Mockito.verify(userSynchronizedList1, Mockito.times(1)).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getUserById and the internal user list throws an
    // InternalServerErrorException while iterating over it, it must rethrow the exception.
    @Test(expected = InternalServerErrorException.class)
    public void testGetUserByIdListThrowsException() throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        Mockito.when(userSynchronizedList1.getNext(Mockito.anyInt())).
        thenReturn(user1).
        thenThrow(new InternalServerErrorException());

        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.getUserById(USER_ID_2, PROVIDER_ID_2);
    }
    
    // test case: When calling the method getUserById and the UserId passed as parameter is 
    // not known, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetUserByIdUnknownUser() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.getUserById("unknownuser", PROVIDER_ID_1);
    }
    
    // test case: When calling the method getUserById and the ProviderId passed as parameter 
    // is not known, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetUserByIdUnknownProvider() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.getUserById(USER_ID_1, "unknownprovider");
    }
    
    // test case: When calling the method getRegisteredUsersByPaymentType, it must return
    // the list of users that use the given payment type.
    @Test
    public void testGetRegisteredUsersByPaymentType() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        assertEquals(userSynchronizedList1, objectHolder.getRegisteredUsersByPaymentType(PLUGIN_NAME_1));
        assertEquals(userSynchronizedList2, objectHolder.getRegisteredUsersByPaymentType(PLUGIN_NAME_2));
    }
    
    // test case: When calling the method getRegisteredUsersByPaymentType passing as argument
    // a payment type not used by any FinanceUser, it must return an empty list.
    @Test
    public void testGetRegisteredUsersWithPaymentTypeNotUsedByAnyUser() throws InternalServerErrorException, ModifiedListException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        MultiConsumerSynchronizedList<FinanceUser> returnedList = 
                objectHolder.getRegisteredUsersByPaymentType("notusedpaymenttype");
        
        Integer consumerId = returnedList.startIterating();
        
        assertNull(returnedList.getNext(consumerId));
    }
    
    // test case: When calling the method changeOptions, it must change the options
    // used by the given user using the data contained in the given options map and
    // then persist the changes using the DatabaseManager.
    @Test
    public void testChangeOptions() throws InvalidParameterException, InternalServerErrorException {
        HashMap<String, String> newOptions = new HashMap<String, String>();
        newOptions.put("option1", "optionvalue1");
        newOptions.put("option2", "optionvalue2");
         
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.changeOptions(USER_ID_1, PROVIDER_ID_1, newOptions);
        
        Mockito.verify(user1).setProperty("option1", "optionvalue1");
        Mockito.verify(user1).setProperty("option2", "optionvalue2");
        Mockito.verify(databaseManager).saveUser(user1);
    }
    
    // test case: When calling the method saveUser, it must persist the user
    // data using the DatabaseManager.
    @Test
    public void testSaveUser() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.saveUser(user1);
        
        Mockito.verify(databaseManager).saveUser(user1);
    }
    
    // test case: When calling the method saveUser and the user is not known, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testSaveUserUnknownUser() throws InvalidParameterException, InternalServerErrorException {
        FinanceUser unknownUser = Mockito.mock(FinanceUser.class);
        Mockito.when(unknownUser.getId()).thenReturn("unknownuser");
        Mockito.when(unknownUser.getProvider()).thenReturn("unknownprovider");
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.saveUser(unknownUser);
    }
    
    // test case: When calling the method registerFinancePlan, it must add the new 
    // FinancePlan to the list of finance plans and then persist the plan using
    // the DatabaseManager.
    @Test
    public void testRegisterFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        FinancePlan newFinancePlan = Mockito.mock(FinancePlan.class);
        Mockito.when(newFinancePlan.getName()).thenReturn(NEW_PLAN_NAME_1);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.registerFinancePlan(newFinancePlan);
        
        Mockito.verify(planSynchronizedList).addItem(newFinancePlan);
        Mockito.verify(databaseManager).saveFinancePlan(newFinancePlan);
    }

    // test case: When calling the method registerFinancePlan and the
    // FinancePlan passed as argument uses an already used plan name, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testCannotRegisterFinancePlanWithAlreadyUsedName() throws InternalServerErrorException, InvalidParameterException {
        FinancePlan newFinancePlan1 = Mockito.mock(FinancePlan.class);
        Mockito.when(newFinancePlan1.getName()).thenReturn(PLAN_NAME_1);

        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        

        objectHolder.registerFinancePlan(newFinancePlan1);
    }
    
    // test case: When calling the method getFinancePlan, it must iterate 
    // correctly over the plans list and return the correct FinancePlan instance.
    @Test
    public void testGetFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        assertEquals(plan1, objectHolder.getFinancePlan(PLAN_NAME_1));
        assertEquals(plan2, objectHolder.getFinancePlan(PLAN_NAME_2));
        
        Mockito.verify(planSynchronizedList, Mockito.times(2)).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlan and a concurrent modification on the 
    // plans list occurs, it must restart the iteration and return the correct FinancePlan instance.
    @Test
    public void testGetFinancePlanListChanges() throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).
        thenReturn(plan1).
        thenThrow(new ModifiedListException()).
        thenReturn(plan1, plan2);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        assertEquals(plan2, objectHolder.getFinancePlan(PLAN_NAME_2));
        
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlan and the plans list throws an
    // InternalServerErrorException, it must rethrow the exception.
    @Test(expected = InternalServerErrorException.class)
    public void testGetFinancePlanListThrowsException() throws InternalServerErrorException, ModifiedListException, InvalidParameterException {
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).
        thenReturn(plan1).
        thenThrow(new InternalServerErrorException()).
        thenReturn(plan1, plan2);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.getFinancePlan(PLAN_NAME_2);
    }
    
    // test case: When calling the method getFinancePlan passing as argument an unknown
    // plan name, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinancePlanUnknownPlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.getFinancePlan("unknownplan");
    }
    
    // test case: When calling the method getOrDefaultFinancePlan and the given plan
    // exists, it must return the FinancePlan instance.
    @Test
    public void testGetOrDefaultFinancePlanPlanExists() throws InternalServerErrorException, InvalidParameterException, ModifiedListException {
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(PLAN_NAME_1);
        
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        
        assertEquals(plan2, objectHolder.getOrDefaultFinancePlan(PLAN_NAME_2));
    }
    
    // test case: When calling the method getOrDefaultFinancePlan and the given plan
    // does not exist, it must return the default FinancePlan.
    @Test
    public void testGetOrDefaultFinancePlanPlanDoesNotExist() throws InternalServerErrorException, InvalidParameterException, ModifiedListException {
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME)).thenReturn(PLAN_NAME_1);
        
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        // Since we look for the given plan and then look for the default plan, 
        // in this case we need to perform two iterations over the plans list.
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).thenReturn(plan1, plan2, null, plan1, plan2, null);
        
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        
        assertEquals(plan1, objectHolder.getOrDefaultFinancePlan("unknownplan"));
    }
    
    // test case: When calling the method removeFinancePlan, it must remove the given plan
    // from the plans list and delete the plan from the database using the DatabaseManager.
    @Test
    public void testRemoveFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.removeFinancePlan(PLAN_NAME_1);
        
        Mockito.verify(databaseManager).removeFinancePlan(PLAN_NAME_1);
        Mockito.verify(planSynchronizedList).removeItem(plan1);
    }
    
    // test case: When calling the method removeFinancePlan passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveFinancePlanUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        
        objectHolder.removeFinancePlan("unknownplan");
    }
    
    // test case: When calling the method updateFinancePlan, it must call the method 
    // update on the correct FinancePlan instance and then persist the plan data
    // using the DatabaseManager.
    @Test
    public void testUpdateFinancePlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        Map<String, String> updatedPlanInfo = new HashMap<String, String>();

        
        objectHolder.updateFinancePlan(PLAN_NAME_1, updatedPlanInfo);
        
        
        Mockito.verify(plan1).update(updatedPlanInfo);
        Mockito.verify(databaseManager).saveFinancePlan(plan1);
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method updateFinancePlan passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinancePlanUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        Map<String, String> updatedPlanInfo = new HashMap<String, String>();

        
        objectHolder.updateFinancePlan("unknownplan", updatedPlanInfo);
    }
    
    // test case: When calling the method getFinancePlan map, it must return a 
    // Map containing the plan data related to the correct FinancePlan instance.
    @Test
    public void testGetFinancePlanMap() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);
        rulesPlan1 = new HashMap<String, String>();
        Mockito.when(plan1.getRulesAsMap()).thenReturn(rulesPlan1);
        
        
        Map<String, String> returnedMap = objectHolder.getFinancePlanMap(PLAN_NAME_1);
        
        
        assertEquals(rulesPlan1, returnedMap);
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlanMap passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinancePlanMapUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, listFactory, userCreditsFactory);

        objectHolder.getFinancePlanMap("unknownplan");
    }

    private void setUpLists() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        userSynchronizedList1 = Mockito.mock(MultiConsumerSynchronizedList.class);
        userSynchronizedList2 = Mockito.mock(MultiConsumerSynchronizedList.class);
        planSynchronizedList = Mockito.mock(MultiConsumerSynchronizedList.class);
        
        listFactory = Mockito.mock(MultiConsumerSynchronizedListFactory.class);
        Mockito.when(listFactory.getList()).thenReturn(userSynchronizedList1, userSynchronizedList2, 
                planSynchronizedList);
        
        Mockito.when(userSynchronizedList1.getNext(Mockito.anyInt())).thenReturn(user1, null);
        Mockito.when(userSynchronizedList2.getNext(Mockito.anyInt())).thenReturn(user2, null);
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).thenReturn(plan1, plan2, null);
    }

    private void setUpDatabase() {
        databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.getRegisteredUsers()).thenReturn(usersList);
        Mockito.when(databaseManager.getRegisteredFinancePlans()).thenReturn(plansList);
    }

    private void setUpPlans() {
        plan1 = Mockito.mock(FinancePlan.class);
        Mockito.when(plan1.getName()).thenReturn(PLAN_NAME_1);
        
        plan2 = Mockito.mock(FinancePlan.class);
        Mockito.when(plan2.getName()).thenReturn(PLAN_NAME_2);
        
        plansList = new ArrayList<FinancePlan>();
        plansList.add(plan1);
        plansList.add(plan2);
    }

    private void setUpCredits() {
        userCreditsFactory = Mockito.mock(UserCreditsFactory.class);
        
        userCredits1 = Mockito.mock(UserCredits.class);
        Mockito.when(userCredits1.getUserId()).thenReturn(USER_ID_1);
        Mockito.when(userCredits1.getProvider()).thenReturn(PROVIDER_ID_1);
        
        userCredits2 = Mockito.mock(UserCredits.class);
        Mockito.when(userCredits2.getUserId()).thenReturn(USER_ID_2);
        Mockito.when(userCredits2.getProvider()).thenReturn(PROVIDER_ID_2);
        
        creditsList = new ArrayList<UserCredits>();
        creditsList.add(userCredits1);
        creditsList.add(userCredits2);
    }

    private void setUpInvoices() {
        invoice1 = Mockito.mock(Invoice.class);
        Mockito.when(invoice1.getInvoiceId()).thenReturn(INVOICE_ID_1);
        Mockito.when(invoice1.getUserId()).thenReturn(USER_ID_1);
        Mockito.when(invoice1.getProviderId()).thenReturn(PROVIDER_ID_1);
        
        invoice2 = Mockito.mock(Invoice.class);
        Mockito.when(invoice2.getInvoiceId()).thenReturn(INVOICE_ID_2);
        Mockito.when(invoice2.getUserId()).thenReturn(USER_ID_1);
        Mockito.when(invoice2.getProviderId()).thenReturn(PROVIDER_ID_1);        
        
        invoice3 = Mockito.mock(Invoice.class);
        Mockito.when(invoice3.getInvoiceId()).thenReturn(INVOICE_ID_3);
        Mockito.when(invoice3.getUserId()).thenReturn(USER_ID_2);
        Mockito.when(invoice3.getProviderId()).thenReturn(PROVIDER_ID_2);
        
        invoicesList = new ArrayList<Invoice>();
        invoicesList.add(invoice1);
        invoicesList.add(invoice2);
        invoicesList.add(invoice3);
    }

    private void setUpUsers() {
        user1 = Mockito.mock(FinanceUser.class);
        Mockito.when(user1.getFinancePluginName()).thenReturn(PLUGIN_NAME_1);
        Mockito.when(user1.getId()).thenReturn(USER_ID_1);
        Mockito.when(user1.getProvider()).thenReturn(PROVIDER_ID_1);
        user2 = Mockito.mock(FinanceUser.class);
        Mockito.when(user2.getFinancePluginName()).thenReturn(PLUGIN_NAME_2);
        Mockito.when(user2.getId()).thenReturn(USER_ID_2);
        Mockito.when(user2.getProvider()).thenReturn(PROVIDER_ID_2);
        
        usersList = new ArrayList<FinanceUser>();
        usersList.add(user1);
        usersList.add(user2);
    }
}
