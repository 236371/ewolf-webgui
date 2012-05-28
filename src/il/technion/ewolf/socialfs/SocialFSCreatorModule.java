package il.technion.ewolf.socialfs;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.stash.Stash;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Properties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class SocialFSCreatorModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("socialfs.user.name", "eyal");
		defaultProps.setProperty("socialfs.user.username", "eyal@gmail");
		defaultProps.setProperty("socialfs.user.password", "1234");
		
		
		return defaultProps;
	}
	
	public SocialFSCreatorModule() {
		this(new Properties());
	}
	
	public SocialFSCreatorModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public SocialFSCreatorModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		
		bind(SFSFile.class);
		bind(SFSFileFactory.class).in(Scopes.SINGLETON);
		bind(UserIDFactory.class).in(Scopes.SINGLETON);
		bind(SecureRandom.class).toInstance(new SecureRandom());
		bind(SocialFSCreator.class).in(Scopes.SINGLETON);
	}
	
	

	@Provides
	@Named("socialfs.crypto.generator.sigkey")
	KeyPairGenerator provideSignatureKeyPairGenerator(
			SecureRandom rnd,
			@Named("socialfs.crypto.generator.sigkey.algo") String algo,
			@Named("socialfs.crypto.generator.sigkey.len") int keysize) throws NoSuchAlgorithmException {
		
		KeyPairGenerator gen = KeyPairGenerator.getInstance(algo);
		gen.initialize(keysize, rnd);
		return gen;
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.sig.keypair")
	KeyPair provideSignatureKeyPair(@Named("socialfs.crypto.generator.sigkey") KeyPairGenerator gen) {
		return gen.generateKeyPair();
	}
	
	@Provides
	@Named("socialfs.crypto.generator.enckey")
	KeyPairGenerator provideEncryptionKeyPairGenerator(
			SecureRandom rnd,
			@Named("socialfs.crypto.generator.enckey.algo") String algo,
			@Named("socialfs.crypto.generator.enckey.len") int keysize) throws NoSuchAlgorithmException {
		KeyPairGenerator gen = KeyPairGenerator.getInstance(algo);
		gen.initialize(keysize, rnd);
		return gen;
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.enc.keypair")
	KeyPair provideEncryptionKeyPair(@Named("socialfs.crypto.generator.enckey") KeyPairGenerator gen) {
		return gen.generateKeyPair();
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.enc.pubkey")
	PublicKey providePublicEncryptionKey(@Named("socialfs.crypto.enc.keypair") KeyPair keyPair) {
		return keyPair.getPublic();
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.enc.privkey")
	PrivateKey providePrivateEncryptionKey(@Named("socialfs.crypto.enc.keypair") KeyPair keyPair) {
		return keyPair.getPrivate();
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.sig.pubkey")
	PublicKey providePublicSignatureKey(@Named("socialfs.crypto.sig.keypair") KeyPair keyPair) {
		return keyPair.getPublic();
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.sig.privkey")
	PrivateKey providePrivateSignatureKey(@Named("socialfs.crypto.sig.keypair") KeyPair keyPair) {
		return keyPair.getPrivate();
	}
	
	@Provides
	@Named("socialfs.rnd.key")
	Key provideRandomKey(KeyFactory keyFactory) {
		return keyFactory.generate();
	}
	@Provides
	@Named("socialfs.rnd.secretkey")
	SecretKey provideRandomSecretKey(
			SecureRandom rnd,
			@Named("socialfs.crypto.generator.semetric.algo") String algo,
			@Named("socialfs.crypto.generator.semetric.size") int keysize) throws NoSuchAlgorithmException {
		KeyGenerator gen = KeyGenerator.getInstance(algo);
		gen.init(keysize, rnd);
		return gen.generateKey();
	}
	
	@Provides
	@Singleton
	@Named("socialfs.local.profile")
	Profile provideLocalProfile(
			@Named("socialfs.crypto.sig.privkey") PrivateKey privSigKey,
			@Named("socialfs.crypto.sig.pubkey") PublicKey pubSigKey,
			@Named("socialfs.crypto.enc.pubkey") PublicKey pubEncKey,
			@Named("socialfs.user.name") String name,
			@Named("socialfs.rnd.key") Key rndKey,
			Stash stash,
			UserIDFactory uidFactory,
			@Named("socialfs.cache.filecache") Cache<SFSFile> fileCache) throws InvalidKeyException, IOException {
		return new Profile(privSigKey, pubSigKey, pubEncKey, name, rndKey, stash, uidFactory, fileCache);
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.credkey")
	SecretKey provideCredentialsKey(@Named("socialfs.rnd.secretkey") SecretKey rndKey) {
		return rndKey;
	}
	
	@Provides
	@Singleton
	@Named("socialfs.crypto.groupkey")
	SecretKey provideGroupMasterKey(@Named("socialfs.rnd.secretkey") SecretKey rndKey) {
		return rndKey;
	}
	
	@Provides
	@Singleton
	Credentials provideCredentials(
			@Named("socialfs.crypto.credkey") SecretKey credentialsKey,
			@Named("socialfs.crypto.sig.privkey") PrivateKey privSigKey,
			@Named("socialfs.crypto.enc.privkey") PrivateKey privEncKey,
			@Named("socialfs.crypto.groupkey") SecretKey groupsMasterKey,
			@Named("socialfs.local.profile") Profile profile) {
		return new Credentials(credentialsKey, privSigKey, privEncKey, groupsMasterKey, profile);
	}
	
	
	
}
