package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;

import cloud.fogbow.common.exceptions.InternalServerErrorException;

public class MultiConsumerSynchronizedListTest {

    private static final String ITEM_1 = "item1";
    private static final String ITEM_2 = "item2";
    private static final String ITEM_3 = "item3";
    private static final String ITEM_4 = "item4";

    // test case: When calling the method next in an empty
    // list, it must return null.
    @Test
    public void testGetNextFromEmptyList() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        
        Integer consumerId = list.startIterating();
        String item = list.getNext(consumerId);
        list.stopIterating(consumerId);
        
        assertNull(item);
    }
    
    @Test
    public void testAddItemAndGetNext() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        list.addItem(ITEM_3);
        
        Integer consumerId = list.startIterating();
        
        String firstItem = list.getNext(consumerId);
        String secondItem = list.getNext(consumerId);
        String thirdItem = list.getNext(consumerId);
        String fourthItem = list.getNext(consumerId);
        
        list.stopIterating(consumerId);
        
        assertEquals(firstItem, ITEM_1);
        assertEquals(secondItem, ITEM_2);
        assertEquals(thirdItem, ITEM_3);
        assertNull(fourthItem);
    }
    
    // test case: When performing multiple concurrent iterations over a 
    // MultiConsumerSynchronizedList, the iterations must yield the same
    // results, regardless of the order the getNext operations are performed.
    @Test
    public void testAddItemAndGetNextListMultipleConsumers() throws ModifiedListException, InternalServerErrorException {
        // prepare list
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        list.addItem(ITEM_3);
        
        // start multiple iterations
        Integer consumerId1 = list.startIterating();
        Integer consumerId2 = list.startIterating();
        
        // perform iteration using the getNext method
        String firstItemConsumer1 = list.getNext(consumerId1);
        String secondItemConsumer1 = list.getNext(consumerId1);
        String firstItemConsumer2 = list.getNext(consumerId2);
        String secondItemConsumer2 = list.getNext(consumerId2);
        
        String thirdItemConsumer1 = list.getNext(consumerId1);
        String thirdItemConsumer2 = list.getNext(consumerId2);
        String fourthItemConsumer1 = list.getNext(consumerId1);
        String fourthItemConsumer2 = list.getNext(consumerId2);
        
        list.stopIterating(consumerId1);
        list.stopIterating(consumerId2);

        // both iterations yield the same results
        assertEquals(firstItemConsumer1, ITEM_1);
        assertEquals(secondItemConsumer1, ITEM_2);
        assertEquals(thirdItemConsumer1, ITEM_3);
        assertNull(fourthItemConsumer1);
        
        assertEquals(firstItemConsumer2, ITEM_1);
        assertEquals(secondItemConsumer2, ITEM_2);
        assertEquals(thirdItemConsumer2, ITEM_3);
        assertNull(fourthItemConsumer2);
    }
 
    @Test
    public void testRemoveItem() throws ModifiedListException, InternalServerErrorException {
        // add items and check if the list is correct
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        
        Integer consumerId = list.startIterating();
        
        String firstItem = list.getNext(consumerId);
        String secondItem = list.getNext(consumerId);
        String thirdItem = list.getNext(consumerId);
        
        list.stopIterating(consumerId);
        
        assertEquals(firstItem, ITEM_1);
        assertEquals(secondItem, ITEM_2);
        assertNull(thirdItem);
        
        // remove the first item
        list.removeItem(ITEM_1);
        
        // check if the list is correct
        consumerId = list.startIterating();
        
        firstItem = list.getNext(consumerId);
        secondItem = list.getNext(consumerId);
        
        list.stopIterating(consumerId);
        
        assertEquals(firstItem, ITEM_2);
        assertNull(secondItem);
    }
    
    // test case: When calling the getNext method using a consumerId
    // not related to any registered iteration, it must throw an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testGetNextUsingInvalidConsumerId() throws ModifiedListException, InternalServerErrorException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        
        Integer consumerId = list.startIterating();
        list.getNext(consumerId + 1);
    }
    
    // test case: When calling the addItem method, it must
    // reset all iterations being performed. A getNext call 
    // passing a consumerId related to a reset iteration must
    // result in a ModifiedListException being thrown.
    @Test
    public void testAddResetsCurrentIterations() throws InternalServerErrorException, ModifiedListException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        list.addItem(ITEM_3);
        
        Integer consumerId = list.startIterating();
        
        String firstItem = list.getNext(consumerId);
        String secondItem = list.getNext(consumerId);
        
        list.addItem(ITEM_4);
        
        try {
            list.getNext(consumerId);
            Assert.fail("Expected to throw exception.");
        } catch (ModifiedListException e) {
            
        }
        
        try {
            list.getNext(consumerId);
            Assert.fail("Expected to throw exception.");
        } catch (InternalServerErrorException e) {
            
        }
        
        assertEquals(ITEM_1, firstItem);
        assertEquals(ITEM_2, secondItem);
    }
    
    // test case: When calling the removeItem method, it must
    // reset all iterations being performed. A getNext call 
    // passing a consumerId related to a reset iteration must
    // result in a ModifiedListException being thrown.
    @Test
    public void testRemoveResetsCurrentIterations() throws InternalServerErrorException, ModifiedListException {
        MultiConsumerSynchronizedList<String> list = new MultiConsumerSynchronizedList<String>();
        list.addItem(ITEM_1);
        list.addItem(ITEM_2);
        list.addItem(ITEM_3);
        
        Integer consumerId = list.startIterating();
        
        String firstItem = list.getNext(consumerId);
        String secondItem = list.getNext(consumerId);
        
        list.removeItem(ITEM_2);
        
        try {
            list.getNext(consumerId);
            Assert.fail("Expected to throw exception.");
        } catch (ModifiedListException e) {
            
        }
        
        try {
            list.getNext(consumerId);
            Assert.fail("Expected to throw exception.");
        } catch (InternalServerErrorException e) {
            
        }
        
        assertEquals(ITEM_1, firstItem);
        assertEquals(ITEM_2, secondItem);
    }
}
