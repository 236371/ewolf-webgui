package il.technion.ewolf;

import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.dht.storage.AgeLimitedDHTStorage;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.socialfs.SFSFile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.stash.Group;
import il.technion.ewolf.stash.Stash;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class EwolfModule extends AbstractModule {

private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("ewolf.messages.dht.storage.maxage", ""+TimeUnit.HOURS.toMillis(1));
		defaultProps.setProperty("ewolf.messages.dht.storage.validtime", ""+TimeUnit.HOURS.toMillis(1));
		
		return defaultProps;
	}
	
	public EwolfModule() {
		this(new Properties());
	}
	
	public EwolfModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public EwolfModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		
		bind(Wall.class);
		
		bind(SocialNetwork.class)
			.to(Ewolf.class)
			.in(Scopes.SINGLETON);
	}
	
	@Provides//(CheckedProvider.class)
	@Named("ewolf.fs.social_groups")
	@Singleton
	SFSFile provideSocialGroupsFolder(
			@Named("ewolf.fs.social_groups.path") String path,
			@Named("ewolf.fs.social_groups.name") String name,
			@Named("ewolf.fs.root") SFSFile rootFolder) throws Exception {
		
		return rootFolder.getSubFile(path+"/"+name);
	}
	
	@Provides//(CheckedProvider.class)
	@Named("ewolf.fs.wall")
	@Singleton
	SFSFile provideWallFolder(
			@Named("ewolf.fs.wall.path") String path,
			@Named("ewolf.fs.wall.name") String name,
			@Named("ewolf.fs.root") SFSFile rootFolder) throws Exception {
		
		return rootFolder.getSubFile(path+"/"+name);
	}
	
	@Provides//(CheckedProvider.class)
	@Named("ewolf.groups.root")
	@Singleton
	Group provideSocialGroupsFolder(
			Stash stash,
			@Named("ewolf.fs.root") SFSFile rootFolder) throws Exception {
		
		return stash.getGroupFromId(rootFolder.getGroupId());
	}
	
	@Provides//(CheckedProvider.class)
	@Named("ewolf.fs.root")
	@Singleton
	SFSFile provideRootFolder(
			SocialFS socialFS) throws Exception {
		return socialFS.getCredentials().getProfile().getRootFile();
	}

	@Provides
	@Named("ewolf.rnd.postID")
	PostID provideRandomPostID(KeyFactory keyFactory) {
		return new PostID(keyFactory.generate());
	}
	
	@Provides
	@Singleton
	@Named("ewolf.messages.dht")
	DHT provideMessagesDHT(DHT dht, AgeLimitedDHTStorage storage,
			@Named("ewolf.messages.dht.storage.maxage") long dhtStorageMaxAge,
			@Named("ewolf.messages.dht.storage.validtime") long dhtStorageValidTime) {
		
		storage
			.setRereplicate(true)
			.setMaxAge(dhtStorageMaxAge)
			.setValidTime(dhtStorageValidTime)
			.create();
			
		return dht.setName("messages.dht")
			.setStorage(storage)
			.create();
	}
	
}
