package cloud.fogbow.fs.core.plugins.authorization.role;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;

public class RoleAuthorizationPlugin implements AuthorizationPlugin<FsOperation> {
    /*
     * The user IDs used in the policy rules must follow this format, a string
     * containing the regular user ID and the provider ID, separated by dot.
     */
    public static final String USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT = "%s.%s";
    private PolicyInstantiator policyInstantiator;
    private RolePolicy<FsOperation> rolePolicy;
    
    public RoleAuthorizationPlugin() throws ConfigurationErrorException {
        this(new PolicyInstantiator());
    }
    
    public RoleAuthorizationPlugin(PolicyInstantiator policyInstantiator) throws ConfigurationErrorException {
        this.policyInstantiator = policyInstantiator;
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        String path = HomeDir.getPath();
        String policyFilePath = path + policyFileName;
        
        try {
            this.rolePolicy = policyInstantiator.getRolePolicyInstanceFromFile(policyFilePath);
        } catch (WrongPolicyTypeException e) {
            throw new ConfigurationErrorException(String.format(Messages.Exception.WRONG_POLICY_TYPE,
                    e.getExpectedType(), e.getCurrentType()));
        }
        
        this.rolePolicy.validate();
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, FsOperation operation) throws UnauthorizedRequestException {
        if (!this.rolePolicy.userIsAuthorized(getUserConfigurationString(systemUser), operation)) {
            throw new UnauthorizedRequestException(Messages.Exception.USER_DOES_NOT_HAVE_ENOUGH_PERMISSION);
        }

        return true;
    }

    private String getUserConfigurationString(SystemUser systemUser) {
        return String.format(USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, systemUser.getId(), systemUser.getIdentityProviderId());
    }

    @Override
    public void setPolicy(String policyString) throws ConfigurationErrorException {
        try {
            RolePolicy<FsOperation> policy = policyInstantiator.getRolePolicyInstance(policyString);
            policy.validate();

            this.rolePolicy = policy;
            this.rolePolicy.save();
        } catch (WrongPolicyTypeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatePolicy(String policyString) throws ConfigurationErrorException {
        try {
            RolePolicy<FsOperation> policy = policyInstantiator.getRolePolicyInstance(policyString);
            RolePolicy<FsOperation> base = this.rolePolicy.copy();

            base.update(policy);
            base.validate();

            this.rolePolicy = base;
            this.rolePolicy.save();
        } catch (WrongPolicyTypeException e) {
            e.printStackTrace();
        }
    }
}
