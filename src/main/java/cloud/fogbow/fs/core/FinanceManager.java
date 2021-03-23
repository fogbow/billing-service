package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.fs.api.parameters.User;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

public class FinanceManager {
	@VisibleForTesting
	static final String FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR = ",";
	private List<FinancePlugin> financePlugins;
	
	public FinanceManager(DatabaseManager databaseManager) throws ConfigurationErrorException {
		String financePluginsString = PropertiesHolder.getInstance()
				.getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
		ArrayList<FinancePlugin> financePlugins = new ArrayList<FinancePlugin>();

		if (financePluginsString.isEmpty()) {
			throw new ConfigurationErrorException(Messages.Exception.NO_FINANCE_PLUGIN_SPECIFIED);
		}
		
		for (String financePluginClassName : financePluginsString.split(FINANCE_PLUGINS_CLASS_NAMES_SEPARATOR)) {
			financePlugins.add(FinancePluginInstantiator.getFinancePlugin(financePluginClassName, databaseManager));
		}
		
		this.financePlugins = financePlugins;
	}
	
	public FinanceManager(List<FinancePlugin> financePlugins) throws ConfigurationErrorException {
		if (financePlugins.isEmpty()) {
			throw new ConfigurationErrorException(Messages.Exception.NO_FINANCE_PLUGIN_SPECIFIED);
		}
		this.financePlugins = financePlugins;
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
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, authenticatedUser.getId()));
	}

	public void addUser(User user) {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.getName().equals(user.getFinancePluginName())) {
				plugin.addUser(user.getUserId(), user.getProvider(), user.getFinanceOptions());
			}
		}
		
		// TODO handle this case
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
	
	public void removeUser(String userId, String provider) {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.managesUser(userId, provider)) {
				plugin.removeUser(userId, provider);
			}
		}
		
		// TODO handle this case
	}

	public void changeOptions(String userId, String provider, HashMap<String, String> financeOptions) {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.managesUser(userId, provider)) {
				plugin.changeOptions(userId, provider, financeOptions);
			}
		}
		
		// TODO handle this case
	}

	public void updateFinanceState(String userId, String provider, HashMap<String, String> financeState) {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.managesUser(userId, provider)) {
				plugin.updateFinanceState(userId, provider, financeState);
			}
		}
		
		// TODO handle this case
	}

	public String getFinanceStateProperty(String userId, String provider, String property) throws FogbowException {
		for (FinancePlugin plugin : financePlugins) {
			if (plugin.managesUser(userId, provider)) {
				return plugin.getUserFinanceState(userId, provider, property);
			}
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNMANAGED_USER, userId));
	}
}
