package cloud.fogbow.fs;

import java.util.ArrayList;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.ApplicationFacade;
import cloud.fogbow.fs.core.FinanceManager;
import cloud.fogbow.fs.core.FinancePluginInstantiator;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.plugins.FinancePlugin;

@Component
public class Main implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String financePluginsString = PropertiesHolder.getInstance()
				.getProperty(ConfigurationPropertyKeys.FINANCE_PLUGINS_CLASS_NAMES);
		ArrayList<FinancePlugin> financePlugins = new ArrayList<FinancePlugin>();

		// FIXME constant
		for (String financePluginClassName : financePluginsString.split(",")) {
			financePlugins.add(FinancePluginInstantiator.getFinancePlugin(financePluginClassName));
		}

		FinanceManager financeManager = new FinanceManager(financePlugins);

		ApplicationFacade.getInstance().setFinanceManager(financeManager);
	}
}
