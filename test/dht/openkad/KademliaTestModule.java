package dht.openkad;

import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mock;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import dht.KeyFactory;
import dht.SerializerFactory;
import dht.openkad.ops.KadOperationsExecutor;

public class KademliaTestModule extends AbstractModule {

	private KeyFactory keyFactory = spy(new KadKeyFactory());
	private List<KadEndpoint> endpoints = spy(new ArrayList<KadEndpoint>());
	private LocalStorage localStorage = spy(new LocalStorage());
	
	@Mock private KBucketsList kbuckets;
	@Mock private KadNode localNode;
	@Mock private KadOperationsExecutor opExecutor;
	@Mock private KadConnectionDispacher connDispacher;
	
	
	@Override
	protected void configure() {
		bind(KeyFactory.class).toInstance(keyFactory);
		bind(KBucketsList.class).toInstance(kbuckets);
		bind(new TypeLiteral<List<KadEndpoint>>() {})
			.annotatedWith(Names.named("kad.endpoints"))
			.toInstance(endpoints);
		bind(KadNode.class)
			.annotatedWith(Names.named("kad.localnode"))
			.toInstance(localNode);
		bind(LocalStorage.class).toInstance(localStorage);
		bind(KadOperationsExecutor.class).toInstance(opExecutor);
		bind(KadConnectionDispacher.class).toInstance(connDispacher);
		
		bind(Kademlia.class);
	}


	@Provides
	@Singleton
	SerializerFactory provideSerializerFactory() {
		return new ObjectSerializerFactory();
	}
	
	KeyFactory getKeyFactory() {
		return keyFactory;
	}


	KBucketsList getKbuckets() {
		return kbuckets;
	}


	List<KadEndpoint> getEndpoints() {
		return endpoints;
	}


	KadNode getLocalNode() {
		return localNode;
	}


	LocalStorage getLocalStorage() {
		return localStorage;
	}


	KadOperationsExecutor getOpExecutor() {
		return opExecutor;
	}


	KadConnectionDispacher getConnDispacher() {
		return connDispacher;
	}

	
	
}
