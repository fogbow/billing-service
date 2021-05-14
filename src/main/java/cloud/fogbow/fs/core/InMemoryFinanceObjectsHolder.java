package cloud.fogbow.fs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.constants.Messages;
import cloud.fogbow.fs.core.datastore.DatabaseManager;
import cloud.fogbow.fs.core.models.FinancePlan;
import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.Invoice;
import cloud.fogbow.fs.core.plugins.payment.prepaid.UserCreditsFactory;
import cloud.fogbow.fs.core.util.ModifiedListException;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedList;
import cloud.fogbow.fs.core.util.MultiConsumerSynchronizedListFactory;

public class InMemoryFinanceObjectsHolder {
    private DatabaseManager databaseManager;
    private UserCreditsFactory userCreditsFactory;
    private MultiConsumerSynchronizedListFactory listFactory;
    private Map<String, MultiConsumerSynchronizedList<FinanceUser>> usersByPlugin;
    private MultiConsumerSynchronizedList<FinancePlan> financePlans;

    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager) throws InternalServerErrorException {
        this(databaseManager, new MultiConsumerSynchronizedListFactory(), new UserCreditsFactory());
    }

    public InMemoryFinanceObjectsHolder(DatabaseManager databaseManager,
            MultiConsumerSynchronizedListFactory listFactory, UserCreditsFactory userCreditsFactory)
            throws InternalServerErrorException {
        this.userCreditsFactory = userCreditsFactory;
        this.databaseManager = databaseManager;
        this.listFactory = listFactory;

        // read users data
        List<FinanceUser> databaseUsers = this.databaseManager.getRegisteredUsers();
        usersByPlugin = new HashMap<String, MultiConsumerSynchronizedList<FinanceUser>>();

        for (FinanceUser user : databaseUsers) {
            addUserByPlugin(user);
        }

        // read finance plans data
        List<FinancePlan> databaseFinancePlans = this.databaseManager.getRegisteredFinancePlans();
        financePlans = listFactory.getList();

        for (FinancePlan plan : databaseFinancePlans) {
            financePlans.addItem(plan);
        }
    }

    /*
     * 
     * User methods
     * 
     */

    public void registerUser(String userId, String provider, String pluginName, Map<String, String> financeOptions)
            throws InternalServerErrorException, InvalidParameterException {
        checkIfUserExists(userId, provider);

        FinanceUser user = new FinanceUser(financeOptions);
        user.setUserId(userId, provider);
        user.setFinancePluginName(pluginName);
        user.setCredits(userCreditsFactory.getUserCredits(userId, provider));
        user.setInvoices(new ArrayList<Invoice>());

        addUserByPlugin(user);

        this.databaseManager.saveUser(user);
    }

    private void checkIfUserExists(String userId, String provider) throws InternalServerErrorException, 
            InvalidParameterException {
        try {
            getUserById(userId, provider);
        } catch (InvalidParameterException e) {
            return;
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.USER_ALREADY_EXISTS, provider, userId));
    }

    private void addUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        String pluginName = user.getFinancePluginName();

        if (!usersByPlugin.containsKey(pluginName)) {
            usersByPlugin.put(pluginName, this.listFactory.getList());
        }

        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(pluginName);
        pluginUsers.addItem(user);
    }

    public void saveUser(FinanceUser user) throws InvalidParameterException, InternalServerErrorException {
        getUserById(user.getId(), user.getProvider());

        synchronized (user) {
            this.databaseManager.saveUser(user);
        }
    }

    public void removeUser(String userId, String provider)
            throws InvalidParameterException, InternalServerErrorException {
        FinanceUser userToRemove = getUserById(userId, provider);

        synchronized (userToRemove) {
            removeUserByPlugin(userToRemove);
            this.databaseManager.removeUser(userId, provider);
        }
    }

    private void removeUserByPlugin(FinanceUser user) throws InternalServerErrorException {
        MultiConsumerSynchronizedList<FinanceUser> pluginUsers = usersByPlugin.get(user.getFinancePluginName());
        pluginUsers.removeItem(user);
    }

    public FinanceUser getUserById(String id, String provider)
            throws InternalServerErrorException, InvalidParameterException {
        FinanceUser userToReturn = null;

        for (String pluginName : usersByPlugin.keySet()) {
            userToReturn = getUserByIdAndPlugin(id, provider, pluginName);

            if (userToReturn != null) {
                return userToReturn;
            }
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_USER, id, provider));
    }

    private FinanceUser getUserByIdAndPlugin(String id, String provider, String pluginName)
            throws InternalServerErrorException, InvalidParameterException {
        MultiConsumerSynchronizedList<FinanceUser> users = usersByPlugin.get(pluginName);
        Integer consumerId = users.startIterating();
        FinanceUser userToReturn = null;

        while (true) {
            try {
                userToReturn = getUserFromList(id, provider, users, consumerId);
                users.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                users = usersByPlugin.get(pluginName);
                consumerId = users.startIterating();
            } catch (Exception e) {
                users.stopIterating(consumerId);
                throw e;
            }
        }

        return userToReturn;
    }

    private FinanceUser getUserFromList(String id, String provider, MultiConsumerSynchronizedList<FinanceUser> users,
            Integer consumerId) throws ModifiedListException, InternalServerErrorException {
        FinanceUser item = users.getNext(consumerId);
        FinanceUser userToReturn = null;

        while (item != null) {
            if (item.getId().equals(id) && item.getProvider().equals(provider)) {
                userToReturn = item;
                break;
            }

            item = users.getNext(consumerId);
        }

        return userToReturn;
    }

    public MultiConsumerSynchronizedList<FinanceUser> getRegisteredUsersByPaymentType(String pluginName) {
        if (this.usersByPlugin.containsKey(pluginName)) {
            return this.usersByPlugin.get(pluginName);
        } else {
            return new MultiConsumerSynchronizedList<FinanceUser>();
        }
    }

    public void changeOptions(String userId, String provider, Map<String, String> financeOptions)
            throws InvalidParameterException, InternalServerErrorException {
        FinanceUser user = getUserById(userId, provider);

        synchronized (user) {
            for (String option : financeOptions.keySet()) {
                user.setProperty(option, financeOptions.get(option));
            }

            this.databaseManager.saveUser(user);
        }
    }

    /*
     * 
     * FinancePlans methods
     * 
     */

    public void registerFinancePlan(FinancePlan financePlan) throws InternalServerErrorException, InvalidParameterException {
        checkIfPlanExists(financePlan.getName());
        this.financePlans.addItem(financePlan);
        this.databaseManager.saveFinancePlan(financePlan);
    }

    private void checkIfPlanExists(String name) throws InternalServerErrorException, InvalidParameterException {
        try {
            getFinancePlan(name);
        } catch (InvalidParameterException e) {
            return;
        }
        
        throw new InvalidParameterException(String.format(Messages.Exception.FINANCE_PLAN_ALREADY_EXISTS, name));
    }

    public FinancePlan getFinancePlan(String planName) throws InternalServerErrorException, InvalidParameterException {
        Integer consumerId = financePlans.startIterating();
        FinancePlan planToReturn = null;

        while (true) {
            try {
                planToReturn = tryToGetPlanFromList(planName, consumerId);
                financePlans.stopIterating(consumerId);
                break;
            } catch (ModifiedListException e) {
                consumerId = financePlans.startIterating();
            } catch (Exception e) {
                financePlans.stopIterating(consumerId);
                throw e;
            }
        }

        if (planToReturn != null) {
            return planToReturn;
        }

        throw new InvalidParameterException(String.format(Messages.Exception.UNABLE_TO_FIND_PLAN, planName));
    }

    private FinancePlan tryToGetPlanFromList(String planName, Integer consumerId)
            throws ModifiedListException, InternalServerErrorException {
        FinancePlan item = financePlans.getNext(consumerId);
        FinancePlan planToReturn = null;

        while (item != null) {
            if (item.getName().equals(planName)) {
                planToReturn = item;
                break;
            }

            item = financePlans.getNext(consumerId);
        }

        return planToReturn;
    }

    public FinancePlan getOrDefaultFinancePlan(String planName)
            throws InternalServerErrorException, InvalidParameterException {
        try {
            return this.getFinancePlan(planName);
        } catch (InvalidParameterException e) {
            String defaultFinancePlan = PropertiesHolder.getInstance()
                    .getProperty(ConfigurationPropertyKeys.DEFAULT_FINANCE_PLAN_NAME);
            return this.getFinancePlan(defaultFinancePlan);
        }
    }

    public void removeFinancePlan(String planName) throws InvalidParameterException, InternalServerErrorException {
        FinancePlan plan = getFinancePlan(planName);

        synchronized (plan) {
            this.financePlans.removeItem(plan);
            this.databaseManager.removeFinancePlan(planName);
        }
    }

    public void updateFinancePlan(String planName, Map<String, String> planInfo)
            throws InvalidParameterException, InternalServerErrorException {
        FinancePlan financePlan = getFinancePlan(planName);

        synchronized (financePlan) {
            financePlan.update(planInfo);
            saveFinancePlan(financePlan);
        }
    }
    
    private void saveFinancePlan(FinancePlan financePlan) throws InternalServerErrorException, InvalidParameterException {
        this.databaseManager.saveFinancePlan(financePlan);
    }

    public Map<String, String> getFinancePlanMap(String planName)
            throws InvalidParameterException, InternalServerErrorException {
        FinancePlan financePlan = getFinancePlan(planName);

        synchronized (financePlan) {
            return financePlan.getRulesAsMap();
        }
    }
}
