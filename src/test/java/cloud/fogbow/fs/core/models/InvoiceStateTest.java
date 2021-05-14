package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class InvoiceStateTest {

    // test case: When calling the fromValue method, it must return
    // the InvoiceState instance represented by the given String.
    @Test
    public void testFromValue() throws InvalidParameterException {
        assertEquals(InvoiceState.DEFAULTING, InvoiceState.fromValue(InvoiceState.DEFAULTING.getValue()));
        assertEquals(InvoiceState.PAID, InvoiceState.fromValue(InvoiceState.PAID.getValue()));
        assertEquals(InvoiceState.WAITING, InvoiceState.fromValue(InvoiceState.WAITING.getValue()));
    }
    
    // test case: When calling the fromValue method passing as argument
    // a String that does not represent any InvoiceState instance, 
    // it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testFromValueInvalidValue() throws InvalidParameterException {
        InvoiceState.fromValue("invalidvalue");
    }
}
