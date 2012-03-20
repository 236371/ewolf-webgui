package il.technion.ewolf.chunkeeper;

import il.technion.ewolf.chunkeeper.handlers.AbstractHandler;
import il.technion.ewolf.chunkeeper.handlers.GetHandler;
import il.technion.ewolf.chunkeeper.net.Base64JsonSerializer;
import il.technion.ewolf.chunkeeper.net.ChunkeeperSerializer;
import il.technion.ewolf.chunkeeper.storage.ChunkStore;
import il.technion.ewolf.chunkeeper.storage.SimpleChunkStore;
import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.dht.storage.AgeLimitedDHTStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ChunKeeperModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		// the chunk locator dht params
		// for more info, refer to the dht documentation
		defaultProps.setProperty("chunkeeper.dht.storage.maxage", ""+TimeUnit.HOURS.toMillis(1));
		defaultProps.setProperty("chunkeeper.dht.storage.validtime", ""+TimeUnit.HOURS.toMillis(1));
		
		return defaultProps;
	}
	
	public ChunKeeperModule() {
		this(new Properties());
	}
	
	public ChunKeeperModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public ChunKeeperModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		
		bind(ChunkeeperSerializer.class)
			.to(Base64JsonSerializer.class)
			.in(Scopes.SINGLETON);
		
		bind(Chunk.class);
		bind(ChunkLocator.class);
		
		// handlers
		bind(GetHandler.class).in(Scopes.SINGLETON);
		
		bind(ChunkStore.class)
			.to(SimpleChunkStore.class)
			.in(Scopes.SINGLETON);
		
		bind(ChunKeeper.class).in(Scopes.SINGLETON);
	}
	
	@Provides
	@Singleton
	Collection<AbstractHandler> provideHandlers(GetHandler getHandler) {
		List<AbstractHandler> $ = new ArrayList<AbstractHandler>();
		$.add(getHandler);
		return Collections.unmodifiableList($);
	}

	@Provides
	@Singleton
	@Named("chunkeeper.dht")
	DHT provideChunkeeperDHT(DHT dht, AgeLimitedDHTStorage storage,
			@Named("chunkeeper.dht.storage.maxage") long dhtStorageMaxAge,
			@Named("chunkeeper.dht.storage.validtime") long dhtStorageValidTime) {
		
		storage
			.setRereplicate(false)
			.setMaxAge(dhtStorageMaxAge)
			.setValidTime(dhtStorageValidTime)
			.create();
			
		return dht.setName("chunkeeper.dht")
			.setStorage(storage)
			.create();
	}
	
	@Provides
	@Singleton
	@Named("chunkeeper.store.hashalgos")
	String[] provideHashAlgorithms() {
		return new String[] { "MD5" };
	}
	
	@Provides
	@Singleton
	@Named("chunkeeper.handlers.get.path")
	String provideGetHandlerPath() {
		return "/chnukeeper/get";
	}
	
}
