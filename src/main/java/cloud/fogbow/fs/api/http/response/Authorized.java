package cloud.fogbow.fs.api.http.response;

import java.util.Map;

public class Authorized {

    public static final String AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD = "authorized";
    private Boolean authorized;
    
    public Authorized(Map<String, Object> requestResponse) {
        this.authorized = (boolean) requestResponse.get(AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD);
    }
    
    public Authorized(boolean authorized) {
        this.authorized = authorized;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public void setAuthorized(Boolean authorized) {
        this.authorized = authorized;
    }

}
