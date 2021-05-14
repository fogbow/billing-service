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
@PrepareForTest({PropertiesHolder.class})
public class AllowOnlyPermissionTest {
    private static final String PERMISSION_NAME_1 = "permission1";
    private static final String PERMISSION_NAME_2 = "permission2";
    private AllowOnlyPermission permission1;
    private AllowOnlyPermission permission2;
    private Set<OperationType> allowedOperations1 = getOperationTypeSet(OperationType.ADD_USER, 
                                                                  OperationType.CHANGE_OPTIONS);
    private Set<OperationType> allowedOperations2 = getOperationTypeSet(OperationType.CREATE_FINANCE_PLAN, 
            OperationType.GET_FINANCE_STATE);
    private Set<OperationType> noOperation = getOperationTypeSet();
    private Set<OperationType> updatedAllowedOperations = getOperationTypeSet(OperationType.ADD_USER);
    
    // test case: if the list of the allowed operations types contains 
    // the type of the operation passed as argument, the method isAuthorized must
    // return true. Otherwise, it must return false.
    @Test
    public void testIsAuthorized() throws InvalidParameterException {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        checkIsAuthorizedUsesTheCorrectOperations(allowedOperations1);
    }
    
    // test case: if the list of the allowed operations is empty,
    // the method isAuthorized must always return false.
    @Test
    public void testIsAuthorizedNoAuthorizedOperation() throws InvalidParameterException {
        permission1 = new AllowOnlyPermission(noOperation);
        checkIsAuthorizedUsesTheCorrectOperations(noOperation);
    }
    
    // test case: when calling the method setOperationTypes, it must
    // update the operations used by the permission.
    @Test
    public void testSetOperationTypes() throws InvalidParameterException {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        checkIsAuthorizedUsesTheCorrectOperations(allowedOperations1);
        
        permission1.setOperationTypes(getOperationTypeStringSet(updatedAllowedOperations));
        checkIsAuthorizedUsesTheCorrectOperations(updatedAllowedOperations);
    }
    
    // test case: when calling the equals method passing a permission with same name
    // and same operations, it must return true.
    @Test
    public void testEqualsSameNameAndOperations() {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowOnlyPermission(getOperationTypeSet(OperationType.ADD_USER, 
                OperationType.CHANGE_OPTIONS));
        permission2.setName(PERMISSION_NAME_1);
        
        assertTrue(permission1.equals(permission2));
        assertTrue(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing a permission with different name
    // and same operations, it must return false.
    @Test
    public void testEqualsDifferentNamesAndSameOperations() {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowOnlyPermission(getOperationTypeSet(OperationType.ADD_USER, 
                OperationType.CHANGE_OPTIONS));
        permission2.setName(PERMISSION_NAME_2);
        
        assertFalse(permission1.equals(permission2));
        assertFalse(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing a permission with same name and
    // different operations, it must return false.
    @Test
    public void testEqualsSameNameAndDifferentOperations() {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowOnlyPermission(allowedOperations2);
        permission2.setName(PERMISSION_NAME_1);
        
        assertFalse(permission1.equals(permission2));
        assertFalse(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing a permission with different name
    // and different operations, it must return false.
    @Test
    public void testEqualsDifferentNamesAndDifferentOperations() {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowOnlyPermission(allowedOperations2);
        permission2.setName(PERMISSION_NAME_2);
        
        assertFalse(permission1.equals(permission2));
        assertFalse(permission2.equals(permission1));
    }
    
    // test case: when calling the equals method passing an object which is not an
    // instance of AllowOnlyPermission, it must return false.
    @Test
    public void testEqualsDifferentObjectTypes() {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        permission1.setName(PERMISSION_NAME_1);
        
        assertFalse(permission1.equals(new Object()));
    }
    
    // test case: when calling the getOperationsTypes method, it must return a Set
    // containing the names of the operations types used by the permission.
    @Test
    public void testGetOperationsTypes() {
        permission1 = new AllowOnlyPermission(allowedOperations1);
        permission1.setName(PERMISSION_NAME_1);
        
        permission2 = new AllowOnlyPermission(noOperation);
        permission2.setName(PERMISSION_NAME_2);
        
        Set<String> operationsNames1 = permission1.getOperationsTypes();
        Set<String> operationsNames2 = permission2.getOperationsTypes();

        assertEquals(2, operationsNames1.size());
        assertTrue(operationsNames1.contains(OperationType.ADD_USER.getValue()));
        assertTrue(operationsNames1.contains(OperationType.CHANGE_OPTIONS.getValue()));
        
        assertEquals(0, operationsNames2.size());
    }
    
    private void checkIsAuthorizedUsesTheCorrectOperations(Set<OperationType> operations) {
        for (OperationType type : OperationType.values()) {
            FsOperation operation = new FsOperation(type);

            if (operations.contains(type)) {
                assertTrue(permission1.isAuthorized(operation));
            } else {
                assertFalse(permission1.isAuthorized(operation));
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
