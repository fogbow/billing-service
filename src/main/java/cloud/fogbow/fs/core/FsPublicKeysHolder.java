package cloud.fogbow.fs.core;

import java.security.interfaces.RSAPublicKey;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.fs.constants.ConfigurationPropertyKeys;

public class FsPublicKeysHolder {
	private RSAPublicKey asPublicKey;
	private RSAPublicKey accsPublicKey;
	private RSAPublicKey rasPublicKey;
	
	private static FsPublicKeysHolder instance;

	private FsPublicKeysHolder() {
	}

	public static synchronized FsPublicKeysHolder getInstance() {
		if (instance == null) {
			instance = new FsPublicKeysHolder();
		}
		return instance;
	}

	public static void reset() {
		instance = null;
	}

	public RSAPublicKey getAsPublicKey() throws FogbowException {
		if (this.asPublicKey == null) {
			String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
			String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
			this.asPublicKey = PublicKeysHolder.getPublicKey(asAddress, asPort,
					cloud.fogbow.as.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT);
		}
		return this.asPublicKey;
	}
	
	public RSAPublicKey getAccsPublicKey() throws FogbowException {
		if (this.accsPublicKey == null) {
			String accsAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_URL_KEY);
			String accsPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ACCS_PORT_KEY);
			this.accsPublicKey = PublicKeysHolder.getPublicKey(accsAddress, accsPort,
					cloud.fogbow.accs.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT);
		}
		return this.accsPublicKey;
	}
	
	public RSAPublicKey getRasPublicKey() throws FogbowException {
		if (this.rasPublicKey == null) {
			String rasAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_URL_KEY);
			String rasPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_PORT_KEY);
			this.rasPublicKey = PublicKeysHolder.getPublicKey(rasAddress, rasPort,
					cloud.fogbow.ras.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT);
		}
		return this.rasPublicKey;
	}
}
