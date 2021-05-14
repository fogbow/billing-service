package cloud.fogbow.fs.core.util;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpRequestClient.class})
public class AuthenticationServiceClientTest {

	private String asAddress = "http://localhost";
	private String asPort = "5000";
	private String publicKey = "publicKey";
	private String username = "username";
	private String password = "password";
	private String userToken = "token";
	
	private String endpoint;
	private Map<String, String> headers;
	private Map<String, Object> body;
	private HttpResponse response;

	// test case: When calling the getToken method, it must set 
	// up a request correctly and return the correct token.
	@Test
	public void testGetToken() throws FogbowException, URISyntaxException {
		setUpRequest();
		setUpOKResponse();
		
		AuthenticationServiceClient asClient = new AuthenticationServiceClient(asAddress, asPort);
		String returnedToken = asClient.getToken(publicKey, username, password);
		
		assertEquals(userToken, returnedToken);
	}
	
	// test case: When calling the getToken method, if the return code
	// is not 201 or 401 (server or communication error), 
	// it must throw an UnavailableProviderException.
	@Test(expected = UnavailableProviderException.class)
	public void testGetTokenErrorReturnCode() throws FogbowException, URISyntaxException {
		setUpRequest();
		setUpErrorResponse();
		
		AuthenticationServiceClient asClient = new AuthenticationServiceClient(asAddress, asPort);
		asClient.getToken(publicKey, username, password);
	}

	// test case: When calling the getToken method, if the return
	// code is 401 (authentication failed), it must throw an UnauthenticatedUserException.
	@Test(expected = UnauthenticatedUserException.class)
	public void testGetTokenAuthenticationError() throws FogbowException, URISyntaxException {
		setUpRequest();
		setUpAuthenticationErrorResponse();

		AuthenticationServiceClient asClient = new AuthenticationServiceClient(asAddress, asPort);
		asClient.getToken(publicKey, username, password);
	}

	private void setUpRequest() {
		endpoint = String.format("%s:%s/%s", asAddress, asPort, 
				cloud.fogbow.as.api.http.request.Token.TOKEN_ENDPOINT);
		
		headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, AuthenticationServiceClient.AUTHENTICATION_REQUEST_CONTENT_TYPE);
		
		body = new HashMap<String, Object>();
		HashMap<String, String> credentials = new HashMap<>();
		credentials.put(AuthenticationServiceClient.USERNAME_REQUEST_KEY, username);
		credentials.put(AuthenticationServiceClient.PASSWORD_REQUEST_KEY, password);
		body.put(AuthenticationServiceClient.PUBLIC_KEY_REQUEST_KEY, publicKey);
		body.put(AuthenticationServiceClient.CREDENTIALS_REQUEST_KEY, credentials);
	}
	
	private void setUpErrorResponse() throws FogbowException {
		setUpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}
	
	private void setUpOKResponse() throws FogbowException {
		setUpResponse(HttpStatus.SC_CREATED);
	}
	
	private void setUpAuthenticationErrorResponse() throws FogbowException {
		setUpResponse(HttpStatus.SC_UNAUTHORIZED);
	}
	
	private void setUpResponse(int returnCode) throws FogbowException {
		response = Mockito.mock(HttpResponse.class);
		Mockito.when(response.getHttpCode()).thenReturn(returnCode);
		String responseContent = String.format("{%s:%s}", AuthenticationServiceClient.TOKEN_RESPONSE_KEY, userToken);
		Mockito.when(response.getContent()).thenReturn(responseContent);
		
		PowerMockito.mockStatic(HttpRequestClient.class);
		BDDMockito.given(HttpRequestClient.doGenericRequestGenericBody(HttpMethod.POST, endpoint, headers, body)).willReturn(response);
	}
}
