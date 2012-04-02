package il.technion.ewolf.stash;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.ChunKeeperModule;
import il.technion.ewolf.dht.SimpleDHTModule;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class StashTest {
	private static final int BASE_PORT = 10000;
	private List<Injector> injectors = new LinkedList<Injector>();

	@After
	public void cleanup() {
		for (Injector inj: injectors) {
			inj.getInstance(KeybasedRouting.class).shutdown();
			inj.getInstance(HttpConnector.class).shutdown();
		}
		injectors.clear();
	}

	@Test
	public void itShouldCreateAGroup() throws Exception {
		Injector injector = Guice.createInjector(
				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "2")
					.setProperty("openkad.bucket.kbuckets.maxsize", "5")
					.setProperty("openkad.net.udp.port", "5555"),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", "5555"),
				
				new SimpleDHTModule()
					.setProperty("chunkeeper.dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5)),
					
				new ChunKeeperModule()
					.setProperty("chunkeeper.dht.storage.validtime", ""+TimeUnit.SECONDS.toMillis(10))
					.setProperty("chunkeeper.dht.storage.maxage", ""+TimeUnit.SECONDS.toMillis(15)),
				
				new StashModule()
				
		);
		injectors.add(injector);
		
		// start the Keybased routing
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// stating the http connector
		HttpConnector httpConnector = injector.getInstance(HttpConnector.class);
		httpConnector.bind();
		httpConnector.start();
		
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		Stash stash = injector.getInstance(Stash.class);
		
		SecretKey groupMasterKey = KeyGenerator.getInstance("AES").generateKey();
		stash.login(groupMasterKey);
		
		Group g = stash.createGroup();
		
		Assert.assertTrue(stash.getAllGroups().contains(g));
		
	}
	
	
	@Test
	public void itShouldStoreAnObject() throws Exception {
		Injector injector = Guice.createInjector(
				new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "2")
					.setProperty("openkad.bucket.kbuckets.maxsize", "5")
					.setProperty("openkad.net.udp.port", "14000"),
					
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", "14000"),
				
				new SimpleDHTModule()
					.setProperty("chunkeeper.dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5)),
					
				new ChunKeeperModule()
					.setProperty("chunkeeper.dht.storage.validtime", ""+TimeUnit.SECONDS.toMillis(10))
					.setProperty("chunkeeper.dht.storage.maxage", ""+TimeUnit.SECONDS.toMillis(15)),
				
				new StashModule()
				
		);
		injectors.add(injector);
		
		// start the Keybased routing
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();
		
		// stating the http connector
		HttpConnector httpConnector = injector.getInstance(HttpConnector.class);
		httpConnector.bind();
		httpConnector.start();
				
		// bind the chunkeeper
		ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
		chnukeeper.bind();
		
		Stash stash = injector.getInstance(Stash.class);
		
		SecretKey groupMasterKey = KeyGenerator.getInstance("AES").generateKey();
		stash.login(groupMasterKey);
		
		Group g = stash.createGroup();
		Key key = injector.getInstance(KeyFactory.class).generate();
		stash.put(key, "abc", g);
		
		List<LazyChunkDecryptor> res = stash.get(key);
		
		Assert.assertEquals(1, res.size());
		Assert.assertEquals("abc", res.get(0).downloadAndDecrypt(String.class));
	}
	
	
	@Test
	public void itShouldStoreAnObjectWith16Nodes() throws Exception {
		
		List<Stash> stashes = new ArrayList<Stash>();
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		
		for (int i=0; i < 16; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "2")
						.setProperty("openkad.bucket.kbuckets.maxsize", "5")
						.setProperty("openkad.net.udp.port", ""+(BASE_PORT+i)),
						
					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(BASE_PORT+i)),
					
					new SimpleDHTModule()
						.setProperty("chunkeeper.dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5)),
						
					new ChunKeeperModule()
						.setProperty("chunkeeper.dht.storage.validtime", ""+TimeUnit.SECONDS.toMillis(10))
						.setProperty("chunkeeper.dht.storage.maxage", ""+TimeUnit.SECONDS.toMillis(15)),
					
					new StashModule()
					
			);
			injectors.add(injector);
			
			// start the Keybased routing
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			
			// stating the http connector
			HttpConnector httpConnector = injector.getInstance(HttpConnector.class);
			httpConnector.bind();
			httpConnector.start();
					
			// bind the chunkeeper
			ChunKeeper chnukeeper = injector.getInstance(ChunKeeper.class);
			chnukeeper.bind();
			
			Stash stash = injector.getInstance(Stash.class);
			
			SecretKey groupMasterKey = KeyGenerator.getInstance("AES").generateKey();
			stash.login(groupMasterKey);
			
			stashes.add(stash);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = BASE_PORT + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		
		Group g = stashes.get(0).createGroup();
		Key key = kbrs.get(0).getKeyFactory().generate();
		stashes.get(0).put(key, "abc", g);
		
		// sharing the group with stash 5
		stashes.get(5).addGroup(g);
		
		List<LazyChunkDecryptor> res = stashes.get(5).get(key);
		
		Assert.assertEquals(1, res.size());
		Assert.assertEquals("abc", res.get(0).downloadAndDecrypt(String.class));
		
		try {
			res = stashes.get(6).get(key);
			res.get(0).downloadAndDecrypt(String.class);
			Assert.assertTrue("should not be able to decrypt", false);
		} catch (Exception e) {
			Assert.assertEquals(GroupNotFoundException.class, e.getClass());
		}
	}
	
}
