package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class ComputeItemTest {

    @Test
    public void testContructor() throws InvalidParameterException {
        ComputeItem item = new ComputeItem(2, 4);
        
        assertEquals(2, item.getvCPU());
        assertEquals(4, item.getRam());
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testContructorNegativeVcpu() throws InvalidParameterException {
        new ComputeItem(-2, 4);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testContructorNegativeRam() throws InvalidParameterException {
        new ComputeItem(2, -4);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testSetNegativeVcpu() throws InvalidParameterException {
        ComputeItem item = new ComputeItem(2, 4);
        
        item.setvCPU(-2);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testSetNegativeRam() throws InvalidParameterException {
        ComputeItem item = new ComputeItem(2, 4);
        
        item.setRam(-2);
    }
    
    @Test
    public void testToString() throws InvalidParameterException {
        ComputeItem item = new ComputeItem(2, 4);
        
        String expected = String.format("{\"type\":\"compute\", \"vCPU\":%d, \"ram\":%d}", 2, 4);
        
        assertEquals(expected, item.toString());
    }
}
