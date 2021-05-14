package cloud.fogbow.fs.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.PropertiesHolder;
import cloud.fogbow.fs.core.models.OperationType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class AdminAuthorizationPluginTest {

	private static final String USER_ID_1 = "userId1";
	private static final String USER_NAME_1 = "userName1";
	private static final String PROVIDER_1 = "provider1";
	private static final String USER_ID_2 = "userId2";
	private static final String USER_NAME_2 = "userName2";
	private static final String PROVIDER_2 = "provider2";
	private static final String USER_ID_NOT_ADMIN = "userIdNotAdmin";
	private static final String USER_NAME_NOT_ADMIN = "userNameNotAdmin";
	private static final String PROVIDER_NOT_ADMIN = "providerNotAdmin";
	private SystemUser user1;
	private SystemUser user2;
	private SystemUser userNotAdmin;

	// test case: When calling the isAuthorized plugin, it must check if the 
	// user belongs to the list of administrators. If the user belongs, it 
	// must return true.
	@Test
	public void testIsAuthorized() throws ConfigurationErrorException, UnauthorizedRequestException {
		setUpPlugin();
		
		AdminAuthorizationPlugin authorizationPlugin = new AdminAuthorizationPlugin();
		
		user1 = new SystemUser(USER_ID_1, USER_NAME_1, PROVIDER_1);
		user2 = new SystemUser(USER_ID_2, USER_NAME_2, PROVIDER_2);

		assertTrue(authorizationPlugin.isAuthorized(user1, new FsOperation(OperationType.ADD_USER)));
		assertTrue(authorizationPlugin.isAuthorized(user2, new FsOperation(OperationType.ADD_USER)));
	}
	
	// test case: When calling the isAuthorized plugin, it must check if the
	// user belongs to the list of administrators. If the user does not belong, 
	// it must throw an UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class)
	public void testIsAuthorizedUserIsNotAuthorized() throws ConfigurationErrorException, UnauthorizedRequestException {
		setUpPlugin();
		
		AdminAuthorizationPlugin authorizationPlugin = new AdminAuthorizationPlugin();

		userNotAdmin = new SystemUser(USER_ID_NOT_ADMIN, USER_NAME_NOT_ADMIN, PROVIDER_NOT_ADMIN);
		
		authorizationPlugin.isAuthorized(userNotAdmin, new FsOperation(OperationType.ADD_USER));
	}
	
	// test case: When calling the constructor and the list of administrators
	// is empty, it must throw a ConfigurationErrorException.
	@Test(expected = ConfigurationErrorException.class)
	public void testConstructorThrowsExceptionIfNoAdminIsGiven() throws ConfigurationErrorException {
		setUpPluginNoAdmin();
		
		new AdminAuthorizationPlugin();
	}
	
	private void setUpPlugin() {
		String adminsString = String.join(AdminAuthorizationPlugin.SEPARATOR, USER_ID_1, USER_ID_2);

		PowerMockito.mockStatic(PropertiesHolder.class);
		PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.ADMINS_IDS)).thenReturn(adminsString);
		BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
	}
	
	private void setUpPluginNoAdmin() {
		PowerMockito.mockStatic(PropertiesHolder.class);
		PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
		Mockito.when(propertiesHolder.getProperty(
				ConfigurationPropertyKeys.ADMINS_IDS)).thenReturn("");
		BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
	}
}
