package il.technion.ewolf.socialfs;

import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.dht.storage.AgeLimitedDHTStorage;
import il.technion.ewolf.socialfs.cache.SimpleCache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class SocialFSModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("socialfs.crypto.generator.sigkey.algo", "DSA");
		defaultProps.setProperty("socialfs.crypto.generator.sigkey.len", "1024");
		
		defaultProps.setProperty("socialfs.crypto.generator.enckey.algo", "RSA");
		defaultProps.setProperty("socialfs.crypto.generator.enckey.len", "2048");
		
		defaultProps.setProperty("socialfs.crypto.generator.semetric.algo", "AES");
		defaultProps.setProperty("socialfs.crypto.generator.semetric.size", "128");
		
		defaultProps.setProperty("socialfs.cred.encryption.algo", "AES");
		defaultProps.setProperty("socialfs.cred.digest.algo", "MD5");
		
		defaultProps.setProperty("socialfs.folders.init.nrsubfolders", "10");
		
		defaultProps.setProperty("socialfs.profile.dht.storage.maxage", ""+TimeUnit.HOURS.toMillis(1));
		defaultProps.setProperty("socialfs.profile.dht.storage.validtime", ""+TimeUnit.HOURS.toMillis(1));
		
		return defaultProps;
	}
	
	public SocialFSModule() {
		this(new Properties());
	}
	
	public SocialFSModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public SocialFSModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	public Properties getProfileDHTProperties() {
		Properties p = new Properties();
		
		p.setProperty("dht.name", "socialfs.profile.dht");
		p.setProperty("dht.storage.rereplicate", "true");
		p.setProperty("dht.storage.maxage", ""+TimeUnit.DAYS.toMillis(1));
		p.setProperty("dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5));
		
		for (String n : properties.stringPropertyNames()) {
			if (!n.startsWith("socialfs.profile.dht"))
				continue;
			p.put(n.substring("socialfs.profile.".length()), properties.get(n));
		}
		
		return p;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);

		bind(new TypeLiteral<Cache<SFSFile>>() {})
			.annotatedWith(Names.named("socialfs.cache.filecache"))
			.to(new TypeLiteral<SimpleCache<SFSFile>>() {})
			.in(Scopes.SINGLETON);
		
		bind(new TypeLiteral<Cache<Profile>>() {})
			.annotatedWith(Names.named("socialfs.cache.profilecache"))
			.to(new TypeLiteral<SimpleCache<Profile>>() {})
			.in(Scopes.SINGLETON);
		
		bind(SFSFile.class);
		bind(SFSFileFactory.class).in(Scopes.SINGLETON);
		bind(SocialFS.class).in(Scopes.SINGLETON);
	}
	
	@Provides
	@Named("socialfs.cred.digest")
	MessageDigest providePasswordDigester(@Named("socialfs.cred.digest.algo") String algo) throws NoSuchAlgorithmException {
		return MessageDigest.getInstance(algo);
	}
	
	@Provides
	@Singleton
	@Named("socialfs.profile.dht")
	DHT provideProfileDHT(DHT dht, AgeLimitedDHTStorage storage,
			@Named("socialfs.profile.dht.storage.maxage") long dhtStorageMaxAge,
			@Named("socialfs.profile.dht.storage.validtime") long dhtStorageValidTime) {
		
		storage
			.setRereplicate(true)
			.setMaxAge(dhtStorageMaxAge)
			.setValidTime(dhtStorageValidTime)
			.create();
			
		return dht.setName("profile.dht")
			.setStorage(storage)
			.create();
	}
	
	
}
