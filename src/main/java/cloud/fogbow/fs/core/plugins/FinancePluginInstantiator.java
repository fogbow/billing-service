package cloud.fogbow.fs.core.plugins;

import cloud.fogbow.fs.core.FsClassFactory;
import cloud.fogbow.fs.core.InMemoryFinanceObjectsHolder;

public class FinancePluginInstantiator {
	private static FsClassFactory classFactory = new FsClassFactory();
	
    public static FinancePlugin getFinancePlugin(String className, InMemoryFinanceObjectsHolder objectHolder) {
        return (FinancePlugin) classFactory.createPluginInstance(className, objectHolder);
    }
}
