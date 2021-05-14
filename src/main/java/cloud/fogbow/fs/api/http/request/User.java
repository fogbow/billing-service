package cloud.fogbow.fs.api.http.request;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fs.constants.SystemConstants;
import cloud.fogbow.fs.core.ApplicationFacade;

// TODO documentation
@CrossOrigin
@RestController
@RequestMapping(value = User.USER_ENDPOINT)
public class User {
	public static final String USER_ENDPOINT = SystemConstants.SERVICE_BASE_ENDPOINT + "user";

	// TODO documentation
	@RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Boolean> addUser(
    		@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
    		@RequestBody cloud.fogbow.fs.api.parameters.User user) throws FogbowException {
        ApplicationFacade.getInstance().addUser(systemUserToken, user);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
	
	// TODO documentation
	@RequestMapping(value = "/{provider}/{userId}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> removeUser(
    		@PathVariable String userId,
    		@PathVariable String provider,
    		@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken) throws FogbowException {
        ApplicationFacade.getInstance().removeUser(systemUserToken, userId, provider);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }
	
	// TODO documentation
	@RequestMapping(value = "/{provider}/{userId}", method = RequestMethod.PUT)
	public ResponseEntity<Boolean> changeOptions(
			@PathVariable String userId,
			@PathVariable String provider,
			@RequestHeader(value = SystemConstants.SYSTEM_USER_TOKEN_HEADER_KEY) String systemUserToken,
			@RequestBody HashMap<String, String> financeOptions) throws FogbowException {
		ApplicationFacade.getInstance().changeOptions(systemUserToken, userId, provider, financeOptions);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
}
