package cloud.fogbow.fs.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.fs.constants.Messages;

public class MultiConsumerSynchronizedList<T> {
    private List<T> internalList;
    private Map<Integer, Integer> pointers;
    private Integer currentConsumerIndex;
    
    public MultiConsumerSynchronizedList() {
        internalList = new ArrayList<T>();
        currentConsumerIndex = 0;
        pointers = new HashMap<Integer, Integer>();
    }
    
    public Integer startIterating() {
        synchronized(internalList) { 
            pointers.put(currentConsumerIndex, 0);
            return currentConsumerIndex++;
        }
    }
    
    public T getNext(Integer consumerIndex) throws ModifiedListException, InternalServerErrorException {
        synchronized(internalList) {
            if (!pointers.containsKey(consumerIndex)) {
                throw new InternalServerErrorException(Messages.Exception.INVALID_CONSUMER_INDEX);
            }
            
            Integer pointer = pointers.get(consumerIndex);
            
            if (pointer.equals(-1)) {
                pointers.remove(consumerIndex);
                throw new ModifiedListException();
            }
            
            if (internalList.size() <= pointer) {
                return null;
            }
            
            T value = internalList.get(pointer);
            pointers.put(consumerIndex, ++pointer);
            return value;
        }
    }
    
    public void stopIterating(Integer consumerIndex) {
        synchronized(internalList) {
            pointers.remove(consumerIndex);
        }
    }
    
    public void addItem(T item) {
        synchronized(internalList) {
            resetPointers();
            internalList.add(item);
        }
    }

    public void removeItem(T item) {
        synchronized(internalList) {
            resetPointers();
            internalList.remove(item);
        }
    }

    private void resetPointers() {
        for (Integer key : pointers.keySet()) {
            pointers.put(key, -1);
        }
    }
}
