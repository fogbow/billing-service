package cloud.fogbow.fs.core.util;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.core.FsPublicKeysHolder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HttpRequestClient.class, ServiceAsymmetricKeysHolder.class, 
	FsPublicKeysHolder.class, TokenProtector.class, CryptoUtil.class })
public class RasClientTest {

	private AuthenticationServiceClient authenticationServiceClient;
	private String rasAddress = "http://localhost";
	private String rasPort = "5001";
	private String managerUserName = "managerUsername";
	private String managerPassword = "managerPassword";
	private String adminToken = "adminToken";
	private String rewrapAdminToken = "rewrapAdminToken";
	private String publicKey = "publicKey";
	private String rasPauseEndpoint;
	private String rasResumeEndpoint;
	private String userId = "userId";
	private HashMap<String, String> headers;
	private HashMap<String, String> body;
	private HttpResponse responsePause;
	private HttpResponse responseResume;
	
	// test case: When calling the method pauseResourcesByUser, it must set up
	// a request properly and call the HttpRequestClient to make the request to RAS.
	@Test
	public void testPauseResourcesByUser() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndOkResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.pauseResourcesByUser(userId);
		
		PowerMockito.verifyStatic(HttpRequestClient.class);
		HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers, body);
	}
	
	// test case: When calling the method pauseResourcesByUser and the return code
	// for the request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testPauseResourcesByUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndErrorResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.pauseResourcesByUser(userId);
	}
	
	// test case: When calling the method resumeResourcesByUser, it must set up
	// a request properly and call the HttpRequestClient to make the request to RAS.
	@Test
	public void testResumeResourcesByUser() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndOkResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.resumeResourcesByUser(userId);
		
		PowerMockito.verifyStatic(HttpRequestClient.class);
		HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers, body);
	}
	
	// test case: When calling the method resumeResourcesByUser and the return code
	// for the request is not 200, it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testResumeResourcesByUserErrorReturnCode() throws FogbowException, GeneralSecurityException {
		setUpKeys();
		setUpAuthentication();
		setUpRequestAndErrorResponse();
		
		RasClient rasClient = new RasClient(authenticationServiceClient, managerUserName, 
				managerPassword, rasAddress, rasPort);
		
		rasClient.resumeResourcesByUser(userId);
	}
	
	private void setUpKeys() throws InternalServerErrorException, FogbowException, UnauthenticatedUserException,
			GeneralSecurityException {
		RSAPublicKey fsPublicKey = Mockito.mock(RSAPublicKey.class);
		RSAPrivateKey fsPrivateKey = Mockito.mock(RSAPrivateKey.class);

		PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
		ServiceAsymmetricKeysHolder serviceAsymmetricKeysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
		Mockito.when(serviceAsymmetricKeysHolder.getPublicKey()).thenReturn(fsPublicKey);
		Mockito.when(serviceAsymmetricKeysHolder.getPrivateKey()).thenReturn(fsPrivateKey);
		BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(serviceAsymmetricKeysHolder);

		RSAPublicKey rasPublicKey = Mockito.mock(RSAPublicKey.class);

		PowerMockito.mockStatic(FsPublicKeysHolder.class);
		FsPublicKeysHolder fsPublicKeysHolder = Mockito.mock(FsPublicKeysHolder.class);
		Mockito.when(fsPublicKeysHolder.getRasPublicKey()).thenReturn(rasPublicKey);
		BDDMockito.given(FsPublicKeysHolder.getInstance()).willReturn(fsPublicKeysHolder);

		PowerMockito.mockStatic(TokenProtector.class);
		BDDMockito.when(
				TokenProtector.rewrap(fsPrivateKey, rasPublicKey, adminToken, FogbowConstants.TOKEN_STRING_SEPARATOR))
				.thenReturn(rewrapAdminToken);

		PowerMockito.mockStatic(CryptoUtil.class);
		BDDMockito.when(CryptoUtil.toBase64(fsPublicKey)).thenReturn(publicKey);
	}
	
	private void setUpAuthentication() throws FogbowException {
		authenticationServiceClient = Mockito.mock(AuthenticationServiceClient.class);
		Mockito.when(authenticationServiceClient.getToken(publicKey, managerUserName, 
				managerPassword)).thenReturn(adminToken);
	}
	
	private void setUpRequestAndResponse(int returnCode) throws FogbowException {
		headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, AccountingServiceClient.RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapAdminToken);
		body = new HashMap<String, String>();
		
		rasPauseEndpoint = String.format("%s:%s/%s/%s", rasAddress, rasPort,
				cloud.fogbow.ras.api.http.request.Compute.PAUSE_COMPUTE_ENDPOINT, userId);
		rasResumeEndpoint = String.format("%s:%s/%s/%s", rasAddress, rasPort,
				cloud.fogbow.ras.api.http.request.Compute.RESUME_COMPUTE_ENDPOINT, userId);
		
		responsePause = Mockito.mock(HttpResponse.class);
		Mockito.when(responsePause.getHttpCode()).thenReturn(returnCode);
		
		responseResume = Mockito.mock(HttpResponse.class);
		Mockito.when(responseResume.getHttpCode()).thenReturn(returnCode);

		PowerMockito.mockStatic(HttpRequestClient.class);
		
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasPauseEndpoint, headers, body)).thenReturn(responsePause);
		BDDMockito.when(HttpRequestClient.doGenericRequest(HttpMethod.POST, rasResumeEndpoint, headers, body)).thenReturn(responseResume);
	}

	private void setUpRequestAndOkResponse() throws FogbowException {
		setUpRequestAndResponse(200);
	}
	
	private void setUpRequestAndErrorResponse() throws FogbowException {
		setUpRequestAndResponse(501);
	}
}
