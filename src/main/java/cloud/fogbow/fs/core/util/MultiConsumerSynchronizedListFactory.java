package cloud.fogbow.fs.core.util;

public class MultiConsumerSynchronizedListFactory {
    public <T> MultiConsumerSynchronizedList<T> getList() {
        return new MultiConsumerSynchronizedList<T>();
    }
}
