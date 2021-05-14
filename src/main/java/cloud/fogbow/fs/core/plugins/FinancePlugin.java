package cloud.fogbow.fs.core.plugins;

import java.util.Map;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.RasOperation;

/**
 * A {@link cloud.fogbow.fs.core.models.FinanceUser} manager, 
 * capable of managing the state of several users through a set of working
 * threads and providing information on the user state. Since a FinancePlugin
 * also works as a thread manager, this abstraction provides methods for 
 * starting and stopping the internal threads.
 */
public interface FinancePlugin {
	/**
	 * Starts all the internal threads related to this plugin's
	 * financial management. 
	 */
	void startThreads();
	
	/**
	 * Stops all the internal threads related to this plugin's
	 * financial management.
	 */
	void stopThreads();
	
	/**
	 * Verifies if the user represented by {@code userId} is authorized to perform the
	 * operation with the given parameters.
	 * 
	 * @param userId the id of the user to be authorized.
	 * @param operationParameters a map containing the operation parameters.
	 * @return a boolean stating whether the user is authorized or not.
	 * @throws InvalidParameterException if an error occurs while trying to find the user 
	 * to check state.
	 * @throws InternalServerErrorException if an error occurs while trying to retrieve the
	 * user state.
	 */
	boolean isAuthorized(SystemUser user, RasOperation operation) throws InvalidParameterException, InternalServerErrorException;
	
	/**
	 * Verifies if this plugin manages the financial state
	 * of the user represented by {@code userId}.
	 * 
	 * @param userId the id of the user to verify.
	 * @param provider the id of the provider of the user to verify.
	 * @return a boolean stating whether the user is managed by this plugin or not.
	 * @throws InternalServerErrorException if an error occurs while trying to retrieve the user state. 
	 * @throws InvalidParameterException if an error occurs while trying to find the user.
	 */
	boolean managesUser(String userId, String provider) throws InternalServerErrorException, InvalidParameterException;
	
	/**
	 * Generates and returns a representation of the given property of
	 * a user finance state.
	 * 
	 * @param userId the id of the user whose state is used in the operation.
	 * @param provider the id of the provider of the user whose state is used in the operation.
	 * @param property a description of the property to generate.
	 * @return a representation of the property.
	 * @throws InvalidParameterException if an error occurs while trying to find the user or
	 * the property is invalid.
	 * @throws InternalServerErrorException if an error occurs while trying to retrieve the user
	 * state.
	 */
	String getUserFinanceState(String userId, String provider, String property) throws InvalidParameterException, InternalServerErrorException;

	/**
	 * Returns a String representing the FinancePlugin name.
	 * 
	 * @return the name String.
	 */
	String getName();
	
	/**
	 * Adds the given user to the list of the users managed by the FinancePlugin.
	 * 
	 * @param userId the id of the user to be managed.
	 * @param provider the id of the provider of the user to be managed.
	 * @param financeOptions the financial options to follow while managing the user financial state.
	 * @throws InvalidParameterException if the options are invalid.
	 * @throws InternalServerErrorException if an error occurs while trying to add the user.
	 */
	void addUser(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException;
	
	/**
	 * Removes the given user from the list of the users managed by the FinancePlugin.
	 * 
	 * @param userId the id of the user to remove.
	 * @param provider the id of the provider of the user to remove.
	 * @throws InvalidParameterException if an error occurs while trying to find the user.
	 * @throws InternalServerErrorException if an error occurs while trying to remove the user.
	 */
	void removeUser(String userId, String provider) throws InvalidParameterException, InternalServerErrorException;

	/**
	 * Changes the financial options used to manage the user.
	 * 
	 * @param userId the id of the user to update.
	 * @param provider the id of the provider of the user to update.
	 * @param financeOptions the new financial options to follow.
	 * @throws InvalidParameterException if an error occurs while trying to find the user
	 * or the options are invalid.
	 * @throws InternalServerErrorException if an error occurs while trying to update the options.
	 */
	void changeOptions(String userId, String provider, Map<String, String> financeOptions) throws InvalidParameterException, InternalServerErrorException;
	
	/**
	 * Updates the user financial state using the given map.
	 * 
	 * @param userId the id of the user whose state must be updated.
	 * @param provider the id of the provider of the user whose state must be updated.
	 * @param financeState a map containing properties used to update the user state.
	 * @throws InvalidParameterException if an error occurs while trying to find the user
	 * or the financial properties are invalid. 
	 * @throws InternalServerErrorException if an error occurs while trying to update the user state.
	 */
	void updateFinanceState(String userId, String provider, Map<String, String> financeState) throws InvalidParameterException, InternalServerErrorException;
}
