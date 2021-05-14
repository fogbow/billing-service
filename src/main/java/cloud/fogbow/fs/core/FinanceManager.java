package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.plugins.FinancePlugin;
import cloud.fogbow.fs.core.plugins.FinancePluginInstantiator;
import cloud.fogbow.fs.core.util.FinancePlanFactory;

public class FinanceManager {
    @VisibleForTesting
    static final String FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR = ",";
    private List<FinancePlugin> financePlugins;
    private InMemoryFinanceObjectsHolder objectHolder;
    private FinancePlanFactory financePlanFactory;

    public FinanceManager(InMemoryFinanceObjectsHolder objectHolder, FinancePlanFactory financePlanFactory)
            throws ConfigurationErrorException, InternalServerErrorException {
        this.objectHolder = objectHolder;
        this.financePlanFactory = financePlanFactory;

        createDefaultPlanIfItDoesNotExist();
        createFinancePlugins(objectHolder);
    }

    private void createDefaultPlanIfItDoesNotExist() throws InternalServerErrorException, ConfigurationErrorException {
        String defaultFinancePlanName = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME);
        String defaultFinancePlanFilePath = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_FILE_PATH);

        try {
            this.objectHolder.getFinancePlan(defaultFinancePlanName);
        } catch (InvalidParameterException e) {
            tryToCreateDefaultFinancePlan(defaultFinancePlanName, defaultFinancePlanFilePath);
        }
    }

    private void tryToCreateDefaultFinancePlan(String defaultFinancePlanName, String defaultFinancePlanFilePath)
            throws ConfigurationErrorException, InternalServerErrorException {
        try {
            FinancePlan financePlan = this.financePlanFactory.createFinancePlan(defaultFinancePlanName,
                    defaultFinancePlanFilePath);
            this.objectHolder.registerFinancePlan(financePlan);
        } catch (InvalidParameterException e) {
            throw new ConfigurationErrorException(e.getMessage());
        }
    }

    private void createFinancePlugins(InMemoryFinanceObjectsHolder objectHolder) throws ConfigurationErrorException {
        ArrayList<FinancePlugin> financePlugins = new ArrayList<FinancePlugin>();

        String financePluginsString = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);

        if (financePluginsString.isEmpty()) {
            throw new ConfigurationErrorException(Messages.Exception.NO_FINANCE_PLUGIN_SPECIFIED);
        }

        for (String financePluginClassName : financePluginsString.split(FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR)) {
            financePlugins.add(FinancePluginInstantiator.getFinancePlugin(financePluginClassName, objectHolder));
        }

        this.financePlugins = financePlugins;
    }

    public FinanceManager(List<FinancePlugin> financePlugins, InMemoryFinanceObjectsHolder objectHolder,
            FinancePlanFactory financePlanFactory) throws ConfigurationErrorException {
        if (financePlugins.isEmpty()) {
            throw new ConfigurationErrorException(Messages.Exception.NO_FINANCE_PLUGIN_SPECIFIED);
        }
        this.financePlugins = financePlugins;
        this.objectHolder = objectHolder;
        this.financePlanFactory = financePlanFactory;
    }

    public boolean isAuthorized(AuthorizableUser user) throws FogbowException {
        String userToken = user.getUserToken();
        RSAPublicKey rasPublicKey = FsPublicKeysHolder.getInstance().getRasPublicKey();
        SystemUser authenticatedUser = AuthenticationUtil.authenticate(rasPublicKey, userToken);

        for (FinancePlugin plugin : financePlugins) {
            String userId = authenticatedUser.getId();
            String userProvider = authenticatedUser.getIdentityProviderId();
            
            if (plugin.managesUser(userId, userProvider)) {
                return plugin.isAuthorized(authenticatedUser, user.getOperation());
            }
        }

        throw new InvalidParameterException(
                String.format(Messages.Exception.UNMANAGED_USER, authenticatedUser.getId()));
    }

    public void startPlugins() {
        for (FinancePlugin plugin : financePlugins) {
            plugin.startThreads();
        }
    }

    public void stopPlugins() {
        for (FinancePlugin plugin : financePlugins) {
            plugin.stopThreads();
        }
    }
    
    public void resetPlugins() throws ConfigurationErrorException {
        createFinancePlugins(objectHolder);
    }

    /*
     * User Management
     */

    public void addUser(User user) throws InvalidParameterException, InternalServerErrorException {
        for (FinancePlugin plugin : financePlugins) {
            if (plugin.getName().equals(user.getFinancePluginName())) {
                plugin.addUser(user.getUserId(), user.getProvider(), user.getFinanceOptions());
                return;
            }
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, user.getUserId()));
    }

    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        FinancePlugin plugin = getUserPlugin(userId, provider);
        plugin.removeUser(userId, provider);
    }

    public void changeOptions(String userId, String provider, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        FinancePlugin plugin = getUserPlugin(userId, provider);
        plugin.changeOptions(userId, provider, financeOptions);
    }

    public void updateFinanceState(String userId, String provider, Map<String, String> financeState)
            throws InvalidParameterException, InternalServerErrorException {
        FinancePlugin plugin = getUserPlugin(userId, provider);
        plugin.updateFinanceState(userId, provider, financeState);
    }

    public String getFinanceStateProperty(String userId, String provider, String property) throws FogbowException {
        FinancePlugin plugin = getUserPlugin(userId, provider);
        return plugin.getUserFinanceState(userId, provider, property);
    }

    private FinancePlugin getUserPlugin(String userId, String provider) throws InvalidParameterException, 
    InternalServerErrorException {
        for (FinancePlugin plugin : financePlugins) {
            if (plugin.managesUser(userId, provider)) {
                return plugin;
            }
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, userId));
    }

    /*
     * Plan management
     */

    public void createFinancePlan(String planName, Map<String, String> planInfo) throws InvalidParameterException, 
            InternalServerErrorException {
        FinancePlan financePlan = this.financePlanFactory.createFinancePlan(planName, planInfo);
        this.objectHolder.registerFinancePlan(financePlan);
    }

    public Map<String, String> getFinancePlan(String planName) throws InvalidParameterException, InternalServerErrorException {
        return this.objectHolder.getFinancePlanMap(planName);
    }

    public void updateFinancePlan(String planName, Map<String, String> planInfo) throws InvalidParameterException, InternalServerErrorException {
        this.objectHolder.updateFinancePlan(planName, planInfo);
    }

    public void removeFinancePlan(String planName) throws InvalidParameterException, InternalServerErrorException {
        String defaultFinancePlan = PropertiesHolder.getInstance()
                .getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME);

        if (defaultFinancePlan.equals(planName)) {
            throw new InvalidParameterException(Messages.Exception.CANNOT_REMOVE_DEFAULT_FINANCE_PLAN);
        }
        
        this.objectHolder.removeFinancePlan(planName);
    }
}
