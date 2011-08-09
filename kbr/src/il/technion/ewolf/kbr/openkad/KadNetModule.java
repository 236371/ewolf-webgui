package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.net.KadProtocol;
import il.technion.ewolf.kbr.openkad.net.KadServer;

import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class KadNetModule extends AbstractModule {

	
	private final Properties properties;
	
	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();
		
		defaultProps.setProperty("kadnet.seed", "0");
		defaultProps.setProperty("kadnet.bucketsize", "20");
		defaultProps.setProperty("kadnet.srv.buffsize", "65536");
		defaultProps.setProperty("kadnet.srv.conn.port", "0");  // serverport used for establishing a socket connection
		defaultProps.setProperty("kadnet.keyfactory.seed", "0");
		defaultProps.setProperty("kadnet.keyfactory.keysize", "20");
		defaultProps.setProperty("kadnet.keyfactory.hashalgo", "SHA-256");
		defaultProps.setProperty("kadnet.executors.incoming.nrthreads", "2");
		defaultProps.setProperty("kadnet.executors.outgoing.nrthreads", "2");
		defaultProps.setProperty("kadnet.executors.listeners.nrthreads", "2");
		defaultProps.setProperty("kadnet.concurrency", "3");
		defaultProps.setProperty("kadnet.oudpkad.port", "-1");
		defaultProps.setProperty("kadnet.otcpkad.port", "-1");
		defaultProps.setProperty("kadnet.localkey", "");
		defaultProps.setProperty("kadnet.refreshinterval", "6000000");
		//defaultProps.setProperty("kadnet.proxy.checkinterval", "60000");
		
		return defaultProps;
	}
	
	public KadNetModule() {
		this(new Properties());
	}
	
	public KadNetModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}
	
	
	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
		bind(KadServer.class).in(Singleton.class);
		//bind(KadProxyServer.class).in(Singleton.class);
		bind(KBuckets.class).in(Singleton.class);
		//bind(OpenedKadConnections.class).in(Singleton.class);
		bind(KadRefresher.class).in(Singleton.class);
		bind(KadListenersServer.class).in(Singleton.class);
		bind(KeybasedRouting.class).to(KadNet.class).in(Singleton.class);
	}
	
	
	@Provides
	@Singleton
	@Named("kadnet.random")
	Random provideRandom(@Named("kadnet.seed") long seed) {
		return seed == 0 ? new Random() : new Random(seed);
	}
	
	@Provides
	@Singleton
	KeyFactory provideKeyFactory(
			@Named("kadnet.keyfactory.keysize") int keysize,
			@Named("kadnet.keyfactory.seed") int seed,
			@Named("kadnet.keyfactory.hashalgo") String hashAlgo) throws NoSuchAlgorithmException {
		return new KadKeyFactory(keysize, seed, hashAlgo);
	}
	
	@Provides
	@Named("kadnet.executors.incoming")
	@Singleton
	ExecutorService provideIncomingExecutor(@Named("kadnet.executors.incoming.nrthreads") int nrThreads) {
		return Executors.newFixedThreadPool(nrThreads);
	}
	
	@Provides
	@Named("kadnet.executors.outgoing")
	@Singleton
	ExecutorService provideOutgoingExecutor(@Named("kadnet.executors.outgoing.nrthreads") int nrThreads) {
		return Executors.newFixedThreadPool(nrThreads);
	}
	
	@Provides
	@Named("kadnet.executors.listeners")
	@Singleton
	ExecutorService provideListenersExecutor(@Named("kadnet.executors.listeners.nrthreads") int nrThreads) {
		return Executors.newFixedThreadPool(nrThreads);
	}
	
	@Provides
	@Named("kadnet.localnode")
	@Singleton
	KadNode provideLocalNode(
			@Named("kadnet.random") Random rnd,
			@Named("kadnet.localkey") String keyString,
			KeyFactory keyFactory,
			@Named("kadnet.otcpkad.port") int otcpkadPort,
			@Named("kadnet.oudpkad.port") int oudpkadPort) throws Exception {
		
		Key key = (keyString.isEmpty()) ? keyFactory.generate() : keyFactory.getFromKey(keyString);
		
		List<KadEndpoint> endpoints = new ArrayList<KadEndpoint>();
		
		if (otcpkadPort == 0) {
			otcpkadPort = rnd.nextInt(64512) + 1024;
		}
		if (oudpkadPort == 0) {
			oudpkadPort = rnd.nextInt(64512) + 1024;
		}
		
		if (otcpkadPort > 0) {
			KadEndpoint endpoint = new KadEndpoint(KadProtocol.otcpkad.name(), otcpkadPort);
			endpoints.add(endpoint);
		}
		if (oudpkadPort > 0) {
			KadEndpoint endpoint = new KadEndpoint(KadProtocol.oudpkad.name(), oudpkadPort);
			endpoints.add(endpoint);
		}
		
		return new KadNode(key, InetAddress.getLocalHost(), endpoints);
	}
	

}
