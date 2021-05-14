package cloud.fogbow.fs.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.FsPublicKeysHolder;
import cloud.fogbow.fs.core.PropertiesHolder;

public class RasClient {
	
	private static final String RECORDS_REQUEST_CONTENT_TYPE = "application/json";
	
	private AuthenticationServiceClient authenticationServiceClient;
	private String publicKey;
	private String managerUsername;
	private String managerPassword;
	private String rasAddress;
	private String rasPort;
	
	public RasClient() throws ConfigurationErrorException {
		this(new AuthenticationServiceClient(),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_URL_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_PORT_KEY));
	}
	
	public RasClient(AuthenticationServiceClient authenticationServiceClient, String managerUserName, 
			String managerPassword, String rasAddress, String rasPort) throws ConfigurationErrorException {
		this.authenticationServiceClient = authenticationServiceClient;
		this.managerUsername = managerUserName;
		this.managerPassword = managerPassword;
		this.rasAddress = rasAddress;
		this.rasPort = rasPort;
		
		try {
			this.publicKey = CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
		} catch (InternalServerErrorException e) {
			throw new ConfigurationErrorException(e.getMessage());
		} catch (GeneralSecurityException e) {
			throw new ConfigurationErrorException(e.getMessage());
		}
	}
	
	public void pauseResourcesByUser(String userId) throws FogbowException {
		try {
			// TODO We should not need to get this token in all the calls to pauseUserComputes.
			// I think we should keep the value and reacquire the token after a certain time.
			String token = authenticationServiceClient.getToken(publicKey, managerUsername, managerPassword);
			Key keyToDecrypt = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
			Key keyToEncrypt = FsPublicKeysHolder.getInstance().getRasPublicKey(); 
			
			String newToken = TokenProtector.rewrap(keyToDecrypt, keyToEncrypt, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
			pauseComputesForUser(userId, newToken);
		} catch (URISyntaxException e) {
			throw new InvalidParameterException(e.getMessage());
		}
	}
	
	private void pauseComputesForUser(String userId, String token) throws URISyntaxException, FogbowException {
		doPauseRequestAndCheckStatus(userId, token);
	}

	private void doPauseRequestAndCheckStatus(String userId, String token) throws URISyntaxException, FogbowException {
		String endpoint = getPauseEndpoint(cloud.fogbow.ras.api.http.request.Compute.PAUSE_COMPUTE_ENDPOINT, userId);
		HttpResponse response = doPauseRequest(token, endpoint);
		if (response.getHttpCode() > HttpStatus.SC_OK) {
			Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
			throw new UnavailableProviderException(e.getMessage());
		}
	}

	private String getPauseEndpoint(String pauseApiBaseEndpoint, String userId) throws URISyntaxException {
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(pauseApiBaseEndpoint).
        		path("/").path(userId).build(true).toUri();
        return uri.toString();
	}
	
	private HttpResponse doPauseRequest(String token, String endpoint) throws FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
	}

	public void resumeResourcesByUser(String userId) throws FogbowException {
		try {
			// TODO We should not need to get this token in all the calls to resumeUserComputes.
			// I think we should keep the value and reacquire the token after a certain time.
			String token = authenticationServiceClient.getToken(publicKey, managerUsername, managerPassword);
			Key keyToDecrypt = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
			Key keyToEncrypt = FsPublicKeysHolder.getInstance().getRasPublicKey(); 
			
			String newToken = TokenProtector.rewrap(keyToDecrypt, keyToEncrypt, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
			resumeComputesForUser(userId, newToken);
		} catch (URISyntaxException e) {
			throw new InvalidParameterException(e.getMessage());
		}
	}

	private void resumeComputesForUser(String userId, String token) throws URISyntaxException, FogbowException {
		doResumeRequestAndCheckStatus(userId, token);
	}
	
	private void doResumeRequestAndCheckStatus(String userId, String token) throws URISyntaxException, FogbowException {
		String endpoint = getResumeEndpoint(cloud.fogbow.ras.api.http.request.Compute.RESUME_COMPUTE_ENDPOINT, userId);
		HttpResponse response = doResumeRequest(token, endpoint);
		if (response.getHttpCode() > HttpStatus.SC_OK) {
			Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
			throw new UnavailableProviderException(e.getMessage());
		}
	}
	
	private String getResumeEndpoint(String resumeApiBaseEndpoint, String userId) throws URISyntaxException {
		URI uri = new URI(rasAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(rasPort).path(resumeApiBaseEndpoint).path("/").
        		path(userId).build(true).toUri();
        return uri.toString();
	}
	
	private HttpResponse doResumeRequest(String token, String endpoint) throws FogbowException {
		// header
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
		headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
		
		// body
		Map<String, String> body = new HashMap<String, String>();

		return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
	}
}
