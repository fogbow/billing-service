package cloud.fogbow.fs.core;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.OperationType;
import cloud.fogbow.fs.core.plugins.authorization.FsOperation;
import cloud.fogbow.fs.core.util.SynchronizationManager;

public class ApplicationFacade {
	
	private static Logger LOGGER = Logger.getLogger(ApplicationFacade.class);
	private static ApplicationFacade instance;
	private FinanceManager financeManager;
	private AuthorizationPlugin<FsOperation> authorizationPlugin;
	private SynchronizationManager synchronizationManager;
	
	private ApplicationFacade() {
		
	}

	public void setAuthorizationPlugin(AuthorizationPlugin<FsOperation> authorizationPlugin) {
		this.authorizationPlugin = authorizationPlugin;
	}
	
	public static ApplicationFacade getInstance() {
		if (instance == null) {
			instance = new ApplicationFacade();
		}
		return instance;
	}

	public void setFinanceManager(FinanceManager financeManager) { 
		this.financeManager = financeManager;
	}

	public void setSynchronizationManager(SynchronizationManager synchronizationManager) {
		this.synchronizationManager = synchronizationManager;
	}
	
	public String getPublicKey() throws InternalServerErrorException {
		synchronizationManager.startOperation();
        // There is no need to authenticate the user or authorize this operation
        try {
            return CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
        } catch (GeneralSecurityException e) {
            throw new InternalServerErrorException(e.getMessage());
        } finally {
			synchronizationManager.finishOperation(); 
		}
	}
	
	public boolean isAuthorized(AuthorizableUser user) throws FogbowException {
		synchronizationManager.startOperation();
		
		try { 
			return this.financeManager.isAuthorized(user);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void addUser(String userToken, User user) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.ADDING_USER, user.getUserId()));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.ADD_USER));
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.addUser(user);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void removeUser(String userToken, String userId, String provider) throws UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.REMOVING_USER, userId));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.REMOVE_USER));
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.removeUser(userId, provider);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void changeOptions(String userToken, String userId, String provider, HashMap<String, String> financeOptions) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.CHANGING_OPTIONS, userId));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.CHANGE_OPTIONS));
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.changeOptions(userId, provider, financeOptions);			
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void updateFinanceState(String userToken, String userId, String provider, HashMap<String, String> financeState) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.UPDATING_FINANCE_STATE, userId));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.UPDATE_FINANCE_STATE));
		synchronizationManager.startOperation();
		
		try {
			this.financeManager.updateFinanceState(userId, provider, financeState);			
		} finally {
			synchronizationManager.finishOperation();
		}
	}
	
	public String getFinanceStateProperty(String userToken, String userId, String provider, String property) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.GETTING_FINANCE_STATE, userId, property));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.GET_FINANCE_STATE));
		synchronizationManager.startOperation();

		try {
			return this.financeManager.getFinanceStateProperty(userId, provider, property);
		} finally {
			synchronizationManager.finishOperation();
		}
	}
	
	public void createFinancePlan(String userToken, String planName, Map<String, String> planInfo) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.CREATING_FINANCE_PLAN, planName));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.CREATE_FINANCE_PLAN));
		synchronizationManager.startOperation();

		try {
			this.financeManager.createFinancePlan(planName, planInfo);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public Map<String, String> getFinancePlan(String userToken, String planName) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.GETTING_FINANCE_PLAN, planName));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.GET_FINANCE_PLAN));
		synchronizationManager.startOperation();

		try {
			return this.financeManager.getFinancePlan(planName);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void updateFinancePlan(String userToken, String planName,
			Map<String, String> planInfo) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.UPDATING_FINANCE_PLAN, planName));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.UPDATE_FINANCE_PLAN));
		synchronizationManager.startOperation();

		try {
			this.financeManager.updateFinancePlan(planName, planInfo);
		} finally {
			synchronizationManager.finishOperation();
		}
	}

	public void removeFinancePlan(String userToken, String planName) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.REMOVING_FINANCE_PLAN, planName));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.REMOVE_FINANCE_PLAN));
		synchronizationManager.startOperation();

		try {
			this.financeManager.removeFinancePlan(planName);
		} finally {
			synchronizationManager.finishOperation();
		}
	}
	
	public void reload(String userToken) throws UnauthenticatedUserException, UnauthorizedRequestException, FogbowException {
		LOGGER.info(String.format(Messages.Log.RELOADING_CONFIGURATION));
		
		authenticateAndAuthorize(userToken, new FsOperation(OperationType.RELOAD));
		synchronizationManager.setAsReloading();
		
		try {
			synchronizationManager.waitForRequests();
	        
			LOGGER.info(Messages.Log.STOPPING_FINANCE_PLUGINS);
			this.financeManager.stopPlugins();
			
			LOGGER.info(Messages.Log.RELOADING_PROPERTIES_HOLDER);
			PropertiesHolder.reset();
			
			LOGGER.info(Messages.Log.RELOADING_PUBLIC_KEYS_HOLDER);
	        FsPublicKeysHolder.reset();
	        
	        LOGGER.info(Messages.Log.RELOADING_FS_KEYS_HOLDER);
	        String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
	        String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
	        ServiceAsymmetricKeysHolder.reset(publicKeyFilePath, privateKeyFilePath);
			
	        LOGGER.info(Messages.Log.RELOADING_AUTHORIZATION_PLUGIN);
			this.authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin();

			LOGGER.info(Messages.Log.RELOADING_FINANCE_PLUGINS);
			this.financeManager.resetPlugins();
			this.financeManager.startPlugins();
		} finally {
			synchronizationManager.finishReloading();
		}
	}
	
    public void setPolicy(String userToken, String policy) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.SETTING_POLICY));
        
        authenticateAndAuthorize(userToken, new FsOperation(OperationType.SET_POLICY));
        synchronizationManager.setAsReloading();
        
        try {
            synchronizationManager.waitForRequests();
            this.authorizationPlugin.setPolicy(policy);
            
        } finally {
            synchronizationManager.finishReloading();
        }
    }

    public void updatePolicy(String userToken, String policy) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.UPDATING_POLICY));
        
        authenticateAndAuthorize(userToken, new FsOperation(OperationType.UPDATE_POLICY));
        synchronizationManager.setAsReloading();
        
        try {
            synchronizationManager.waitForRequests();
            this.authorizationPlugin.updatePolicy(policy);
            
        } finally {
            synchronizationManager.finishReloading();
        }
    }
	
	private void authenticateAndAuthorize(String userToken, FsOperation operation)
			throws FogbowException, UnauthenticatedUserException, UnauthorizedRequestException {
		RSAPublicKey asPublicKey = FsPublicKeysHolder.getInstance().getAsPublicKey();
        SystemUser systemUser = AuthenticationUtil.authenticate(asPublicKey, userToken);
        this.authorizationPlugin.isAuthorized(systemUser, operation);
	}
}
