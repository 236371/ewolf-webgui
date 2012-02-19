package il.technion.ewolf.chunkeeper;

import il.technion.ewolf.chunkeeper.handlers.AbstractHandler;
import il.technion.ewolf.chunkeeper.storage.ChunkStore;
import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.kbr.Key;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class ChunKeeper {

	private final DHT chucksDHT;
	private final Provider<Chunk> chunkProvider;
	private final ChunkStore chunkStore;
	private final Collection<AbstractHandler> handlers;
	private final HttpConnector connector;
	
	@Inject
	ChunKeeper(
			@Named("chunkeeper.dht") DHT chucksDHT,
			Provider<Chunk> chunkProvider,
			ChunkStore chunkStore,
			Collection<AbstractHandler> handlers,
			HttpConnector connector) {
		
		this.chucksDHT = chucksDHT;
		this.chunkProvider = chunkProvider;
		this.chunkStore = chunkStore;
		this.handlers = handlers;
		this.connector = connector;
	}
	
	public void bind() {
		for (AbstractHandler h : handlers) {
			h.register(connector);
		}
	}
	
	public void login(KeyPair creditFor) {
		
	}
	
	public void store(Key key, Serializable data) {
		ByteArrayOutputStream bout = null;
		ObjectOutputStream oout = null;
		try {
			bout = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(bout);
			oout.writeObject(data);
			oout.flush();
			chunkStore.store(key, bout.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException("could not serialize chunk", e);
		} finally {
			try { bout.close(); } catch (Exception e) {}
			try { oout.close(); } catch (Exception e) {}
		}
	}
	
	public Set<Chunk> findChunk(Key key) {
		Chunk localResults = chunkStore.get(key);
		if (localResults != null) {
			return Collections.singleton(localResults);
		}
		List<Serializable> searchResults = chucksDHT.get(key);
		Set<Chunk> $ = new HashSet<Chunk>();
		for (Serializable s : searchResults) {
			
			if (!ChunkLocator.class.equals(s.getClass()))
				continue;
			
			ChunkLocator locator = (ChunkLocator)s;
			
			boolean wasAdded = false;
			for (Chunk c : $) {
				if (c.addLocator(locator)) {
					wasAdded = true;
					break;
				}
			}
			if (!wasAdded) {
				Chunk c = chunkProvider.get();
				c.addLocator(locator);
				$.add(c);
			}
		}
		return $;
	}
	
	/*
	public static void main(String[] args) throws Exception {
		
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
		keyGen.initialize(1024);
		KeyPair keyPair = keyGen.generateKeyPair();
		
		System.out.println(keyPair.getPrivate().getClass());
		
		Injector injector = Guice.createInjector(
				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "2")
					.setProperty("openkad.bucket.kbuckets.maxsize", "5")
					.setProperty("openkad.net.udp.port", "5555"),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", "5555"),
				
				new ChunKeeperModule()
		);
		
		// start the Keybased routing
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		HttpConnector connector = injector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		Key k = kbr.getKeyFactory().generate();
		
		chnukeeper.store(k, "abcdefg");
		
		Set<Chunk> chunks = chnukeeper.findChunk(k);
		
		for (Chunk c : chunks) {
			System.out.println(c.download());
		}
		
	}
	*/
	
}
