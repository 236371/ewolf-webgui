package il.technion.ewolf.stash;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;

import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class StashModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("stash.encryption.algo", "AES");
		
		return defaultProps;
	}
	
	public StashModule() {
		this(new Properties());
	}
	
	public StashModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public StashModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		
		bind(Group.class);
		bind(LazyChunkDecryptor.class);
		bind(Stash.class).in(Scopes.SINGLETON);
	}
	
	@Provides
	@Named("stash.random.key")
	Key provideRandomKey(KeyFactory keyFactory) {
		return keyFactory.generate();
	}
	
	@Provides
	@Named("stash.random.secretkey")
	SecretKey provideRandomSecretKey(@Named("stash.encryption.algo") String algo) throws NoSuchAlgorithmException {
		KeyGenerator gen = KeyGenerator.getInstance(algo);
		return gen.generateKey();
	}
	
}
