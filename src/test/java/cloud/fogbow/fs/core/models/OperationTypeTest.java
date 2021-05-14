package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class OperationTypeTest {

    // test case: When calling the fromString method, it
    // must return the OperationType instance represented
    // by the given String.
    @Test
    public void testFromValue() throws InvalidParameterException {
        assertEquals(OperationType.RELOAD, OperationType.fromString(OperationType.RELOAD.getValue()));
        assertEquals(OperationType.ADD_USER, OperationType.fromString(OperationType.ADD_USER.getValue()));
        assertEquals(OperationType.REMOVE_USER, OperationType.fromString(OperationType.REMOVE_USER.getValue()));
        assertEquals(OperationType.CHANGE_OPTIONS, OperationType.fromString(OperationType.CHANGE_OPTIONS.getValue()));
        assertEquals(OperationType.UPDATE_FINANCE_STATE, OperationType.fromString(OperationType.UPDATE_FINANCE_STATE.getValue()));
        assertEquals(OperationType.GET_FINANCE_STATE, OperationType.fromString(OperationType.GET_FINANCE_STATE.getValue()));
        assertEquals(OperationType.CREATE_FINANCE_PLAN, OperationType.fromString(OperationType.CREATE_FINANCE_PLAN.getValue()));
        assertEquals(OperationType.GET_FINANCE_PLAN, OperationType.fromString(OperationType.GET_FINANCE_PLAN.getValue()));
        assertEquals(OperationType.UPDATE_FINANCE_PLAN, OperationType.fromString(OperationType.UPDATE_FINANCE_PLAN.getValue()));
        assertEquals(OperationType.REMOVE_FINANCE_PLAN, OperationType.fromString(OperationType.REMOVE_FINANCE_PLAN.getValue()));
    }
    
    // test case: When calling the fromString method passing as argument
    // a String that does not represent any OperationType instance, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testFromValueInvalidValue() throws InvalidParameterException {
        OperationType.fromString("invalidvalue");
    }
}
