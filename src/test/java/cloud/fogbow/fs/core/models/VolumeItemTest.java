package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class VolumeItemTest {

    @Test
    public void testConstructor() throws InvalidParameterException {
        VolumeItem item = new VolumeItem(100);
        
        assertEquals(100, item.getSize());
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testConstructorNegativeSize() throws InvalidParameterException {
        new VolumeItem(-100);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testSetNegativeSize() throws InvalidParameterException {
        VolumeItem item = new VolumeItem(100);
        
        item.setSize(-100);
    }
    
    @Test
    public void testToString() throws InvalidParameterException {
        VolumeItem item = new VolumeItem(100);
        
        String expected = String.format("{\"type\":\"volume\", \"size\":%d}", 100);
        
        assertEquals(expected, item.toString());
    }
}
