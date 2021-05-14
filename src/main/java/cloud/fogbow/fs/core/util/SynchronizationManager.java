package cloud.fogbow.fs.core.util;

public class SynchronizationManager {
	
	private boolean reloading;
	private long onGoingRequests;

	public SynchronizationManager() {
		this.onGoingRequests = 0L;
		this.reloading = false;
	}
	
	public void setAsReloading() {
        this.reloading = true;
    }
    
    public void finishReloading() {
    	this.reloading = false;
    }
	
    public void startOperation() {
        while (reloading)
            ;
        synchronized (this) {
            this.onGoingRequests++;
        }
    }
    
    public synchronized void finishOperation() {
        this.onGoingRequests--;
    }
    
    public void waitForRequests() {
    	while (this.onGoingRequests != 0) {
    		try {
    			Thread.sleep(10);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    	}
    }
}
