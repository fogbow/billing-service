package cloud.fogbow.fs.core.plugins.authorization.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ PropertiesHolder.class })
public class AllowAllExceptPermissionTest {
    private static final String PERMISSION_NAME_1 = "permission1";
    private static final String PERMISSION_NAME_2 = "permission2";
    private AllowAllExceptPermission permission1;
    private AllowAllExceptPermission permission2;
    private Set<OperationType> notAllowedOperationsPermission1 = getOperationTypeSet(OperationType.ADD_USER, 
            OperationType.CREATE_FINANCE_PLAN);
    private Set<OperationType> notAllowedOperationsPermission2 = getOperationTypeSet(OperationType.CHANGE_OPTIONS, 
            OperationType.REMOVE_FINANCE_PLAN);
    private Set<OperationType> updatedNotAllowedOperations =  getOperationTypeSet(OperationType.ADD_USER);
    private Set<OperationType> noOperation = getOperationTypeSet();

    // test case: if the list of the not allowed operations types contains
    // the type of the operation passed as argument, the method isAuthorized must
    // return false. Otherwise, it must return true.
    @Test
    public void testIsAuthorized() throws InvalidParameterException {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        checkIsAuthorizedUsesTheCorrectOperations(notAllowedOperationsPermission1);
    }

    // test case: if the list of the not allowed operations is empty,
    // the method isAuthorized must always return true.
    @Test
    public void testIsAuthorizedAllOperationsAreAuthorized() throws InvalidParameterException {
        permission1 = new AllowAllExceptPermission(noOperation);
        checkIsAuthorizedUsesTheCorrectOperations(noOperation);
    }
    
    // test case: when calling the method setOperationTypes, it must
    // update the operations used by the permission.
    @Test
    public void testSetOperationTypes() throws InvalidParameterException {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        checkIsAuthorizedUsesTheCorrectOperations(notAllowedOperationsPermission1);
        
        permission1.setOperationTypes(getOperationTypeStringSet(updatedNotAllowedOperations));
        checkIsAuthorizedUsesTheCorrectOperations(updatedNotAllowedOperations);
    }
    
    // test case: when calling the equals method passing a permission with same name
    // and same operations, it must return true.
    @Test
    public void testEqualsSameNameAndOperations() {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowAllExceptPermission(getOperationTypeSet(OperationType.ADD_USER, 
                OperationType.CREATE_FINANCE_PLAN));
        permission2.setName(PERMISSION_NAME_1);
        
        assertTrue(permission1.equals(permission2));
        assertTrue(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing a permission with different name
    // and same operations, it must return false.
    @Test
    public void testEqualsDifferentNamesAndSameOperations() {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowAllExceptPermission(getOperationTypeSet(OperationType.ADD_USER, 
                OperationType.CREATE_FINANCE_PLAN));
        permission2.setName(PERMISSION_NAME_2);
        
        assertFalse(permission1.equals(permission2));
        assertFalse(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing a permission with same name and
    // different operations, it must return false.
    @Test
    public void testEqualsSameNameAndDifferentOperations() {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowAllExceptPermission(notAllowedOperationsPermission2);
        permission2.setName(PERMISSION_NAME_1);
        
        assertFalse(permission1.equals(permission2));
        assertFalse(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing a permission with different name
    // and different operations, it must return false.
    @Test
    public void testEqualsDifferentNamesAndDifferentOperations() {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowAllExceptPermission(notAllowedOperationsPermission2);
        permission2.setName(PERMISSION_NAME_2);
        
        assertFalse(permission1.equals(permission2));
        assertFalse(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing an object which is not an
    // instance of AllowAllExceptPermission, it must return false.
    @Test
    public void testEqualsDifferentObjectTypes() {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        permission1.setName(PERMISSION_NAME_1);
        
        assertFalse(permission1.equals(new Object()));
    }
    
    // test case: when calling the getOperationsTypes method, it must return a Set
    // containing the names of the operations types used by the permission.
    @Test
    public void testGetOperationsTypes() {
        permission1 = new AllowAllExceptPermission(notAllowedOperationsPermission1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowAllExceptPermission(noOperation);
        permission2.setName(PERMISSION_NAME_2);
        
        Set<String> operationsNames1 = permission1.getOperationsTypes();
        Set<String> operationsNames2 = permission2.getOperationsTypes();

        assertEquals(2, operationsNames1.size());
        assertTrue(operationsNames1.contains(OperationType.ADD_USER.getValue()));
        assertTrue(operationsNames1.contains(OperationType.CREATE_FINANCE_PLAN.getValue()));
        
        assertEquals(0, operationsNames2.size());
    }
    
    private void checkIsAuthorizedUsesTheCorrectOperations(Set<OperationType> operations) {
        for (OperationType type : OperationType.values()) {
            FsOperation operation = new FsOperation(type);

            if (operations.contains(type)) {
                assertFalse(permission1.isAuthorized(operation));
            } else {
                assertTrue(permission1.isAuthorized(operation));
            }
        }
    }
    
    private Set<OperationType> getOperationTypeSet(OperationType ... operationTypes) {
        Set<OperationType> operationSet = new HashSet<OperationType>();
        
        for (OperationType operationType : operationTypes) {
            operationSet.add(operationType);
        }
        
        return operationSet;
    }
    
    private Set<String> getOperationTypeStringSet(Set<OperationType> operationTypes) {
        Set<String> operationStrings = new HashSet<String>();
        
        for (OperationType operationType : operationTypes) {
            operationStrings.add(operationType.getValue());
        }
        
        return operationStrings;
    }
}
