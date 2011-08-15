package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.KeybasedRouting;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class DHTModule extends AbstractModule {

	private final Properties properties;
	private final KeybasedRouting kbr;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		
		defaultProps.setProperty("dht.name", "dht");
		defaultProps.setProperty("dht.seed", "0");
		defaultProps.setProperty("dht.get.timeout", "1000");
		defaultProps.setProperty("dht.replication.factor", "2");
		defaultProps.setProperty("dht.replication.saftymargin", "3");
		defaultProps.setProperty("dht.executor.nrthreads", "1");
		
		return defaultProps;
	}
	
	public DHTModule(KeybasedRouting kbr) {
		this(kbr, new Properties());
	}
	
	public DHTModule(KeybasedRouting kbr, Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
		this.kbr = kbr;
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(KeybasedRouting.class).toInstance(kbr);
		bind(DHT.class);
	}
	
	@Provides
	@Singleton
	@Named("dht.random")
	Random provideRandom(@Named("dht.seed") long seed) {
		return seed == 0 ? new Random() : new Random(seed);
	}
	
	@Provides
	@Singleton
	@Named("dht.executor")
	ExecutorService provideExecutorService(@Named("dht.executor.nrthreads") int nrthreads) {
		return Executors.newFixedThreadPool(nrthreads);
	}
}
