package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.KeybasedRouting;

import java.io.File;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

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
		
		defaultProps.setProperty("dht.storage.maxlifetime", ""+(1000*60*60)); //one hour
		defaultProps.setProperty("dht.storage.maxentrysize", ""+(1024*64)); // 64k
		defaultProps.setProperty("dht.storage.maxentries", ""+(1024*1024*64)); //64MB
		defaultProps.setProperty("dht.storage.hibernate.cfg", "conf/hibernate.cfg.xml");
		
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
		bind(DHTStorage.class);
		bind(KeybasedRouting.class).toInstance(kbr);
		bind(DHT.class);
	}
	
	@Provides
	SessionFactory provideSessionFactory(@Named("dht.storage.hibernate.cfg") String hibernateConf) {
		return new Configuration().configure(new File(hibernateConf)).buildSessionFactory();	
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
