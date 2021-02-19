package cloud.fogbow.fs.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.accs.api.http.response.Record;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.CommonKeys;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fs.core.PropertiesHolder;

public class AccountingServiceClient {
	private static final String COMPUTE_RESOURCE = "compute";
	private static final String NETWORK_RESOURCE = "network";
	private static final String VOLUME_RESOURCE = "volume";
	private static final List<String> RESOURCE_TYPES = Arrays.asList(COMPUTE_RESOURCE, 
			NETWORK_RESOURCE, VOLUME_RESOURCE);
	@VisibleForTesting
	static final String RECORDS_REQUEST_CONTENT_TYPE = "application/json";

	private AuthenticationServiceClient authenticationServiceClient;
	private String managerUserName;
	private String managerPassword;
	private String publicKey;
	private String accountingServiceAddress;
	private String accountingServicePort;
	private String localProvider;
	private JsonUtils jsonUtils;
	
	public AccountingServiceClient() throws ConfigurationErrorException {
		this(new AuthenticationServiceClient(), PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PUBLIC_KEY_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY),
				new JsonUtils());
	}
	
	public AccountingServiceClient(AuthenticationServiceClient authenticationServiceClient, 
			String publicKey, String localProvider, String managerUserName, 
			String managerPassword, String accountingServiceAddress, String accountingServicePort, 
			JsonUtils jsonUtils) {
		this.authenticationServiceClient = authenticationServiceClient;
		this.publicKey = publicKey;
		this.localProvider = localProvider;
		this.managerUserName = managerUserName;
		this.managerPassword = managerPassword;
		this.accountingServiceAddress = accountingServiceAddress;
		this.accountingServicePort = accountingServicePort;
		this.jsonUtils = jsonUtils;
	}
	
	public List<Record> getUserRecords(String userId, String requester, String startDate, String endDate) throws FogbowException {
		// FIXME complete this list
		// TODO This implementation does not look very efficient. We should
		// try to find another solution, maybe adding a more powerful 
		// API method to ACCS
		List<Record> userRecords = new ArrayList<Record>();
		
		try {
			String token = authenticationServiceClient.getToken(publicKey, managerUserName, managerPassword);
		
			for (String resourceType : RESOURCE_TYPES) {
				// TODO should rewrap the token before the request
				HttpResponse response = doRequestAndCheckStatus(token, userId, requester, localProvider, resourceType, startDate, endDate);
				userRecords.addAll(getRecordsFromResponse(response));
			}
		} catch (URISyntaxException e) {
			// TODO Improve
			throw new FogbowException(e.getMessage());
		}
		
		return userRecords;
	}
	
    private HttpResponse doRequestAndCheckStatus(String token, String userId, String requester, 
    		String localProvider, String resourceType, String startDate, String endDate) throws URISyntaxException, FogbowException {
        String endpoint = getAccountingEndpoint(cloud.fogbow.accs.api.http.request.ResourceUsage.USAGE_ENDPOINT, 
        		userId, requester, localProvider, resourceType, startDate, endDate);
        HttpResponse response = doRequest(token, endpoint);
        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }
    
    // http://{accs-address}:{accs-port}/accs/usage/{userId}/{requester-provider}/{local-provider}/{resource-type}/{start-date}/{end-date}
    private String getAccountingEndpoint(String path, String userId, String requester, 
    		String localProvider, String resourceType, String startDate, String endDate) throws URISyntaxException {
        URI uri = new URI(accountingServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(accountingServicePort).path(path).path("/").
        		path(userId).path("/").path(requester).path("/").path(localProvider).path("/").
        		path(resourceType).path("/").path(startDate).path("/").path(endDate).
                build(true).toUri();
        return uri.toString();
    }
    
    private HttpResponse doRequest(String token, String endpoint) throws URISyntaxException, FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, RECORDS_REQUEST_CONTENT_TYPE);
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
        // body
        Map<String, String> body = new HashMap<String, String>();
        
        return HttpRequestClient.doGenericRequest(HttpMethod.GET, endpoint, headers, body);
    }
    
    private List<Record> getRecordsFromResponse(HttpResponse response) {
        ArrayList<Record> records = this.jsonUtils.fromJson(response.getContent(), ArrayList.class);
        return records;
    }
}
