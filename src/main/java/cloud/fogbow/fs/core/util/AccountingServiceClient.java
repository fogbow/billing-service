package cloud.fogbow.fs.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.annotations.VisibleForTesting;

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
import cloud.fogbow.fs.core.util.accounting.Record;
import cloud.fogbow.fs.core.util.accounting.RecordUtils;

public class AccountingServiceClient {
    // This string represents the date format
    // expected by the AccountingService, as
    // specified in the RecordService class. The format
    // is specified through a private field, which
    // I think should be made public to possible
    // clients of ACCS' API.
    static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";
	private static final String COMPUTE_RESOURCE = "compute";
	private static final String VOLUME_RESOURCE = "volume";
	private static final List<String> RESOURCE_TYPES = Arrays.asList(COMPUTE_RESOURCE, VOLUME_RESOURCE);
	@VisibleForTesting
	static final String RECORDS_REQUEST_CONTENT_TYPE = "application/json";

	private AuthenticationServiceClient authenticationServiceClient;
	private String managerUserName;
	private String managerPassword;
	private String accountingServiceAddress;
	private String accountingServicePort;
	private String localProvider;
	private String publicKeyString;
	private RecordUtils recordUtil;
	private TimeUtils timeUtils;
	
	public AccountingServiceClient() throws ConfigurationErrorException {
		this(new AuthenticationServiceClient(),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_USERNAME_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MANAGER_PASSWORD_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY),
				PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY),
				new RecordUtils(), new TimeUtils());
	}
	
	public AccountingServiceClient(AuthenticationServiceClient authenticationServiceClient, 
			String localProvider, String managerUserName, String managerPassword, 
			String accountingServiceAddress, String accountingServicePort, RecordUtils recordUtil, 
			TimeUtils timeUtils) 
					throws ConfigurationErrorException {
		this.authenticationServiceClient = authenticationServiceClient;
		this.localProvider = localProvider;
		this.managerUserName = managerUserName;
		this.managerPassword = managerPassword;
		this.accountingServiceAddress = accountingServiceAddress;
		this.accountingServicePort = accountingServicePort;
		this.recordUtil = recordUtil;
		this.timeUtils = timeUtils;
		
		try {
			this.publicKeyString = CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
		} catch (InternalServerErrorException e) {
			throw new ConfigurationErrorException(e.getMessage());
		} catch (GeneralSecurityException e) {
			throw new ConfigurationErrorException(e.getMessage());
		}
	}
	
    public List<Record> getUserRecords(String userId, String requester, long startTime, long endTime) throws FogbowException {
        List<Record> userRecords = new ArrayList<Record>();

        try {
            String requestStartDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, startTime);
            String requestEndDate = this.timeUtils.toDate(SIMPLE_DATE_FORMAT, endTime);
            
            // TODO We should not need to get this token in all the calls to getUserRecords
            // I think we should keep the value and reacquire the token after a certain time
            String token = authenticationServiceClient.getToken(publicKeyString, managerUserName, managerPassword);
            Key keyToDecrypt = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
            Key keyToEncrypt = FsPublicKeysHolder.getInstance().getAccsPublicKey(); 
            
            String newToken = TokenProtector.rewrap(keyToDecrypt, keyToEncrypt, token, FogbowConstants.TOKEN_STRING_SEPARATOR);

            // TODO This implementation does not look very efficient. We should
            // try to find another solution, maybe adding a more powerful 
            // API method to ACCS
            for (String resourceType : RESOURCE_TYPES) {
                HttpResponse response = doRequestAndCheckStatus(newToken, userId, requester, localProvider, resourceType, 
                        requestStartDate, requestEndDate);
                userRecords.addAll(getRecordsFromResponse(response));
            }
        } catch (URISyntaxException e) {
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
    
    private List<Record> getRecordsFromResponse(HttpResponse response) throws InvalidParameterException {
    	return recordUtil.getRecordsFromString(response.getContent());
    }
}
