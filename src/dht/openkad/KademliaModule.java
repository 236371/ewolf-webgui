package dht.openkad;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import dht.DHT;
import dht.KeyFactory;
import dht.SerializerFactory;
import dht.openkad.KadNode.Contact;
import dht.openkad.comm.KadProtocol;
import dht.openkad.ops.KadOperationsExecutor;

public class KademliaModule extends AbstractModule {

	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("kad.bucketsize", "20");
		defaultProps.setProperty("kad.incomming.threadpoolsize", "5");
		defaultProps.setProperty("kad.operations.threadpoolsize", "2");
		defaultProps.setProperty("kad.concurrency", "3");
		defaultProps.setProperty("kad.keyfactory.seed", "0");
		defaultProps.setProperty("kad.endpoint.tcpkad.port", "2357");
		
		return defaultProps;
	}
	
	public KademliaModule() {
		this(new Properties());
	}
	
	public KademliaModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		
		bind(LocalStorage.class).in(Singleton.class);
		bind(KBucketsList.class).in(Singleton.class);
		bind(KadOperationsExecutor.class);
		bind(KadConnectionDispacher.class);
		bind(DHT.class).to(Kademlia.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	SerializerFactory provideSerializerFactory() {
		return new ObjectSerializerFactory();
	}
	
	@Provides
	@Singleton
	KeyFactory provideKadKeyFactory(@Named("kad.keyfactory.seed") long seed) {
		return new KadKeyFactory(seed);
	}
	@Provides
	@Named("kad.incomming.threadpool")
	@Singleton
	ExecutorService provideIncommingExecutor(@Named("kad.incomming.threadpoolsize") int threadpoolSize) {
		return Executors.newFixedThreadPool(threadpoolSize);
	}
	
	
	@Provides
	@Named("kad.operations.threadpool")
	@Singleton
	ExecutorService provideOperationsExecutor(@Named("kad.operations.threadpoolsize") int threadpoolSize) {
		return Executors.newFixedThreadPool(threadpoolSize);
	}
	
	
	@Provides
	@Named("kad.endpoints")
	@Singleton
	List<KadEndpoint> provideEndpoints(@Named("kad.localnode") KadNode localNode) {
		
		List<KadEndpoint>endpoints = new ArrayList<KadEndpoint>();
		for (Contact c : localNode.getContacts()) {
			endpoints.add(KadProtocol.valueOf(c.getProtocol()).createEndpoing(new InetSocketAddress(c.getPort())));
		}
		return endpoints;
	}
	
	@Provides
	@Named("kad.localnode")
	@Singleton
	KadNode provideLocalNode(KeyFactory keyFactory, @Named("kad.endpoint.tcpkad.port") int tcpkadPort) {
		KadNode localNode = new KadNode(keyFactory,
				new Contact(KadProtocol.tcpkad.name(), tcpkadPort));
		return localNode;
	}
	

}
