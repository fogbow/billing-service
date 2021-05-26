package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.PlanPlugin;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

// TODO review documentation
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class InMemoryFinanceObjectsHolderTest {

    private static final String PLAN_NAME_1 = "plan1";
    private static final String PLAN_NAME_2 = "plan2";
    private static final String NEW_PLAN_NAME_1 = "newplan1";
    
    private DatabaseManager databaseManager;
    private MultiConsumerSynchronizedListFactory listFactory;
    private MultiConsumerSynchronizedList<PlanPlugin> planSynchronizedList;

    private List<PlanPlugin> plansList;
    private PlanPlugin plan1;
    private PlanPlugin plan2;
    private Map<String, String> rulesPlan1;
    private InMemoryUsersHolder usersHolder;
    private InMemoryFinanceObjectsHolder objectHolder;
    
    @Before
    public void setUp() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        setUpPlans();
        setUpDatabase();
        setUpLists();
    }
    
    // test case: When creating a new InMemoryFinanceObjectsHolder instance, the constructor
    // must acquire the data from FinanceUsers and FinancePlans using the DatabaseManager and
    // prepare its internal data holding lists properly.
    @Test
    public void testConstructorSetsUpDataStructuresCorrectly() throws InternalServerErrorException, InvalidParameterException, ConfigurationErrorException {
        new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory);
        
        Mockito.verify(databaseManager).getRegisteredPlanPlugins();
        Mockito.verify(listFactory).getList();
        Mockito.verify(planSynchronizedList).addItem(plan1);
        Mockito.verify(planSynchronizedList).addItem(plan2);
    }
    
    // TODO documentation
    @Test
    public void testResetSetsUpDataStructuresCorrectly() throws InternalServerErrorException, ConfigurationErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.reset();
        
        Mockito.verify(databaseManager).getRegisteredPlanPlugins();
        Mockito.verify(listFactory).getList();
        Mockito.verify(planSynchronizedList).addItem(plan1);
        Mockito.verify(planSynchronizedList).addItem(plan2);
    }
    
    // test case: When calling the method registerFinancePlan, it must add the new 
    // FinancePlan to the list of finance plans and then persist the plan using
    // the DatabaseManager.
    @Test
    public void testRegisterFinancePlan() throws InvalidParameterException, InternalServerErrorException, ConfigurationErrorException {
        PlanPlugin planPlugin1 = Mockito.mock(PlanPlugin.class);
        Mockito.when(planPlugin1.getName()).thenReturn(NEW_PLAN_NAME_1);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.registerPlanPlugin(planPlugin1);
        
        Mockito.verify(planSynchronizedList).addItem(planPlugin1);
        Mockito.verify(databaseManager).savePlanPlugin(planPlugin1);
    }

    // test case: When calling the method registerFinancePlan and the
    // FinancePlan passed as argument uses an already used plan name, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testCannotRegisterFinancePlanWithAlreadyUsedName() throws InternalServerErrorException, InvalidParameterException {
        PlanPlugin planPlugin1 = Mockito.mock(PlanPlugin.class);
        Mockito.when(planPlugin1.getName()).thenReturn(PLAN_NAME_1);
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        

        objectHolder.registerPlanPlugin(planPlugin1);
    }
    
    // test case: When calling the method getFinancePlan, it must iterate 
    // correctly over the plans list and return the correct FinancePlan instance.
    @Test
    public void testGetFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        assertEquals(plan1, objectHolder.getPlanPlugin(PLAN_NAME_1));
        assertEquals(plan2, objectHolder.getPlanPlugin(PLAN_NAME_2));
        
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
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        assertEquals(plan2, objectHolder.getPlanPlugin(PLAN_NAME_2));
        
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
        
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.getPlanPlugin(PLAN_NAME_2);
    }
    
    // test case: When calling the method getFinancePlan passing as argument an unknown
    // plan name, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinancePlanUnknownPlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.getPlanPlugin("unknownplan");
    }
    
    // test case: When calling the method removeFinancePlan, it must remove the given plan
    // from the plans list and delete the plan from the database using the DatabaseManager.
    @Test
    public void testRemoveFinancePlan() throws InvalidParameterException, InternalServerErrorException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.removePlanPlugin(PLAN_NAME_1);
        
        Mockito.verify(databaseManager).removePlanPlugin(plan1);
        Mockito.verify(planSynchronizedList).removeItem(plan1);
    }
    
    // test case: When calling the method removeFinancePlan passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testRemoveFinancePlanUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        
        objectHolder.removePlanPlugin("unknownplan");
    }
    
    // test case: When calling the method updateFinancePlan, it must call the method 
    // update on the correct FinancePlan instance and then persist the plan data
    // using the DatabaseManager.
    @Test
    public void testUpdateFinancePlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        Map<String, String> updatedPlanInfo = new HashMap<String, String>();

        
        objectHolder.updatePlanPlugin(PLAN_NAME_1, updatedPlanInfo);
        
        
        Mockito.verify(plan1).setOptions(updatedPlanInfo);
        Mockito.verify(databaseManager).savePlanPlugin(plan1);
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method updateFinancePlan passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testUpdateFinancePlanUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        Map<String, String> updatedPlanInfo = new HashMap<String, String>();

        
        objectHolder.updatePlanPlugin("unknownplan", updatedPlanInfo);
    }
    
    // test case: When calling the method getFinancePlan map, it must return a 
    // Map containing the plan data related to the correct FinancePlan instance.
    @Test
    public void testGetFinancePlanMap() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);
        rulesPlan1 = new HashMap<String, String>();
        Mockito.when(plan1.getOptions()).thenReturn(rulesPlan1);
        
        
        Map<String, String> returnedMap = objectHolder.getPlanPluginOptions(PLAN_NAME_1);
        
        
        assertEquals(rulesPlan1, returnedMap);
        Mockito.verify(planSynchronizedList).stopIterating(Mockito.anyInt());
    }
    
    // test case: When calling the method getFinancePlanMap passing as argument
    // an unknown plan, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testGetFinancePlanMapUnknownPlan() throws InternalServerErrorException, InvalidParameterException {
        objectHolder = new InMemoryFinanceObjectsHolder(databaseManager, usersHolder, listFactory, planSynchronizedList);

        objectHolder.getPlanPluginOptions("unknownplan");
    }

    private void setUpLists() throws InvalidParameterException, ModifiedListException, InternalServerErrorException {
        planSynchronizedList = Mockito.mock(MultiConsumerSynchronizedList.class);
        Mockito.when(planSynchronizedList.getNext(Mockito.anyInt())).thenReturn(plan1, plan2, null);
        
        listFactory = Mockito.mock(MultiConsumerSynchronizedListFactory.class);
        Mockito.doReturn(planSynchronizedList).when(listFactory).getList();
    }

    private void setUpDatabase() {
        databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.getRegisteredPlanPlugins()).thenReturn(plansList);
    }

    private void setUpPlans() {
        plan1 = Mockito.mock(PlanPlugin.class);
        Mockito.when(plan1.getName()).thenReturn(PLAN_NAME_1);
        
        plan2 = Mockito.mock(PlanPlugin.class);
        Mockito.when(plan2.getName()).thenReturn(PLAN_NAME_2);
        
        plansList = new ArrayList<PlanPlugin>();
        plansList.add(plan1);
        plansList.add(plan2);
    }
}
