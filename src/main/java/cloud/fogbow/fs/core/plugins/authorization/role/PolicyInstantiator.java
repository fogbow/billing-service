package cloud.fogbow.fs.core.plugins.authorization.role;

import java.io.File;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.FsClassFactory;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;

public class PolicyInstantiator {
    private FsClassFactory classFactory;
    private String adminRole;
    private String policyFilePath;
    private String policyClass;
    private PermissionInstantiator<FsOperation> permissionInstantiator;
    
    public PolicyInstantiator() throws ConfigurationErrorException {
        this(new FsClassFactory(), new PermissionInstantiator<FsOperation>(new FsClassFactory()));
    }
    
    public PolicyInstantiator(FsClassFactory classFactory, 
            PermissionInstantiator<FsOperation> permissionInstantiator) throws ConfigurationErrorException {
        if (!PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.ADMIN_ROLE)) {
            throw new ConfigurationErrorException(Messages.Exception.ADMIN_ROLE_NOT_SPECIFIED);
        }
        
        if (!PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_FILE_KEY)) { 
            throw new ConfigurationErrorException(Messages.Exception.POLICY_FILE_NAME_NOT_SPECIFIED);
        }
        
        if (!PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) { 
            throw new ConfigurationErrorException(Messages.Exception.POLICY_CLASS_NOT_SPECIFIED);
        }
        
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        
        this.policyFilePath = HomeDir.getPath();
        this.policyFilePath += policyFileName;
        
        this.adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        this.policyClass = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
        this.classFactory = classFactory;
        this.permissionInstantiator = permissionInstantiator;
    }
    
    public RolePolicy<FsOperation> getRolePolicyInstance(String policyString) 
            throws ConfigurationErrorException, WrongPolicyTypeException {
        return (RolePolicy<FsOperation>) this.classFactory.createPluginInstance(this.policyClass, 
                permissionInstantiator, policyString, adminRole, this.policyFilePath);     
    }
    
    public RolePolicy<FsOperation> getRolePolicyInstanceFromFile(String policyFilePath) 
            throws ConfigurationErrorException, WrongPolicyTypeException {
        File policyStartUpFile = new File(policyFilePath);
        return (RolePolicy<FsOperation>) this.classFactory.createPluginInstance(this.policyClass,
                permissionInstantiator, policyStartUpFile, adminRole, policyFilePath);           
    }
}
