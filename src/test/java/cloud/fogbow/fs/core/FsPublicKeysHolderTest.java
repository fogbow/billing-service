package cloud.fogbow.fs.core;

import static org.junit.Assert.assertEquals;

import java.security.interfaces.RSAPublicKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, PublicKeysHolder.class})
public class FsPublicKeysHolderTest {

    private static final String AS_URL = "as_url";
    private static final String AS_PORT = "as_port";
    private static final String ACCS_URL = "accs_url";
    private static final String ACCS_PORT = "accs_port";
    private static final String RAS_URL = "ras_url";
    private static final String RAS_PORT = "ras_port";
    private RSAPublicKey asPublicKey;
    private RSAPublicKey accsPublicKey;
    private RSAPublicKey rasPublicKey;

    @Test
    public void testGetAsPublicKey() throws FogbowException {
        asPublicKey = Mockito.mock(RSAPublicKey.class);
        
        PowerMockito.mockStatic(PublicKeysHolder.class);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.AS_URL_KEY)).thenReturn(AS_URL);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.AS_PORT_KEY)).thenReturn(AS_PORT);
        
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        BDDMockito.given(PublicKeysHolder.getPublicKey(AS_URL, AS_PORT, 
                cloud.fogbow.as.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT)).willReturn(asPublicKey);
        
        assertEquals(asPublicKey, FsPublicKeysHolder.getInstance().getAsPublicKey());
    }
    
    @Test
    public void testGetAccsPublicKey() throws FogbowException {
        accsPublicKey = Mockito.mock(RSAPublicKey.class);
        
        PowerMockito.mockStatic(PublicKeysHolder.class);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY)).thenReturn(ACCS_URL);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY)).thenReturn(ACCS_PORT);
        
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        BDDMockito.given(PublicKeysHolder.getPublicKey(ACCS_URL, ACCS_PORT, 
                cloud.fogbow.accs.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT)).willReturn(accsPublicKey);
        
        assertEquals(accsPublicKey, FsPublicKeysHolder.getInstance().getAccsPublicKey());
    }
    
    @Test
    public void testGetRasPublicKey() throws FogbowException {
        rasPublicKey = Mockito.mock(RSAPublicKey.class);
        
        PowerMockito.mockStatic(PublicKeysHolder.class);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.RAS_URL_KEY)).thenReturn(RAS_URL);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.RAS_PORT_KEY)).thenReturn(RAS_PORT);
        
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        BDDMockito.given(PublicKeysHolder.getPublicKey(RAS_URL, RAS_PORT, 
                cloud.fogbow.ras.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT)).willReturn(rasPublicKey);
        
        assertEquals(rasPublicKey, FsPublicKeysHolder.getInstance().getRasPublicKey());
    }
}
