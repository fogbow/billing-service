package cloud.fogbow.fs.core.models;

import static org.junit.Assert.*;

import org.junit.Test;

import cloud.fogbow.common.exceptions.InvalidParameterException;

public class UserCreditsTest {

    private static final String USER_ID = "userId1";
    private static final String PROVIDER = "provider1";
    private ResourceItem resourceItem;
    private Double itemValue;
    private Double timeUsed;

    @Test
    public void testAddingAndDeductingCredits() throws InvalidParameterException {
        UserCredits userCredits = new UserCredits(USER_ID, PROVIDER);

        // UserCredits starts with 0.0 as credits value
        assertEquals(new Double(0.0), userCredits.getCreditsValue());
        assertEquals(USER_ID, userCredits.getUserId());
        assertEquals(PROVIDER, userCredits.getProvider());
        
        // Adding credits
        userCredits.addCredits(10.0);
        
        assertEquals(new Double(10.0), userCredits.getCreditsValue());
        
        userCredits.addCredits(5.0);
        
        assertEquals(new Double(15.0), userCredits.getCreditsValue());
        
        // Deducting credits
        resourceItem = new VolumeItem(10);
        itemValue = 2.5;
        timeUsed = 2.0;
        
        userCredits.deduct(resourceItem, itemValue, timeUsed);
        
        assertEquals(new Double(10.0), userCredits.getCreditsValue());
        
        userCredits.deduct(resourceItem, itemValue, timeUsed);
        
        assertEquals(new Double(5.0), userCredits.getCreditsValue());
        
        // Deduct credits and make credits value negative
        itemValue = 2.5;
        timeUsed = 3.0;
        
        userCredits.deduct(resourceItem, itemValue, timeUsed);
        
        assertEquals(new Double(-2.5), userCredits.getCreditsValue());
        
        // Add credits and make credits value positive again
        userCredits.addCredits(10.0);
        
        assertEquals(new Double(7.5), userCredits.getCreditsValue());
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testAddNegativeCredits() throws InvalidParameterException {
        UserCredits userCredits = new UserCredits(USER_ID, PROVIDER);
        
        userCredits.addCredits(-1.0);
    }
    
    @Test(expected = InvalidParameterException.class)
    public void testDeductUsingNegativeItemValue() throws InvalidParameterException {
        UserCredits userCredits = new UserCredits(USER_ID, PROVIDER);
        
        resourceItem = new VolumeItem(10);
        timeUsed = 2.0;
        
        userCredits.deduct(resourceItem, -1.0, timeUsed);
    }

    @Test(expected = InvalidParameterException.class)
    public void testDeductUsingNegativeTimeUsed() throws InvalidParameterException {
        UserCredits userCredits = new UserCredits(USER_ID, PROVIDER);
        
        resourceItem = new VolumeItem(10);
        itemValue = 2.5;
        
        userCredits.deduct(resourceItem, itemValue, -2.0);
    }
}
