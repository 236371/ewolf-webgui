package il.technion.ewolf.dht;

import il.technion.ewolf.dht.handlers.FindvalueHandler;
import il.technion.ewolf.dht.handlers.StoreMessageHandler;
import il.technion.ewolf.dht.msg.FindValueRequest;
import il.technion.ewolf.dht.msg.FindValueResponse;
import il.technion.ewolf.dht.msg.StoreMessage;
import il.technion.ewolf.dht.op.GetOperation;
import il.technion.ewolf.dht.op.PutOperation;
import il.technion.ewolf.dht.storage.AgeLimitedDHTStorage;
import il.technion.ewolf.dht.storage.SimpleDHTStorage;

import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class SimpleDHTModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		
		Properties defaultProps = new Properties();
		
		// interval for cleaning the storage from old stale data
		defaultProps.setProperty("dht.storage.checkInterval", ""+TimeUnit.MINUTES.toMillis(5));
		
		return defaultProps;
	}
	
	
	public SimpleDHTModule() {
		this(new Properties());
	}
	
	public SimpleDHTModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	public SimpleDHTModule setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);

		bind(String.class)
			.annotatedWith(Names.named("dht.handlers.name.store"))
			.toInstance("/store");
		
		bind(String.class)
			.annotatedWith(Names.named("dht.handlers.name.findvalue"))
			.toInstance("/findvalue");
		
		bind(Timer.class)
			.toInstance(new Timer());
		
		bind(FindValueRequest.class);
		bind(FindValueResponse.class);
		bind(StoreMessage.class);
		
		bind(StoreMessageHandler.class);
		bind(FindvalueHandler.class);
		
		bind(GetOperation.class);
		bind(PutOperation.class);
		
		bind(AgeLimitedDHTStorage.class);
		bind(SimpleDHTStorage.class);
		
		bind(DHT.class).to(SimpleDHT.class);
	}
}
