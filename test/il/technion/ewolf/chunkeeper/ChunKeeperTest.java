package il.technion.ewolf.chunkeeper;

import il.technion.ewolf.dht.SimpleDHTModule;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;

import ch.lambdaj.Lambda;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ChunKeeperTest {

	
	@Test
	public void itShouldStoreAndRetrieveData() throws Exception {
		int basePort = 10100;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		List<ChunKeeper> chunkeepers = new ArrayList<ChunKeeper>();
		for (int i=0; i < 16; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "2")
						.setProperty("openkad.bucket.kbuckets.maxsize", "5")
						.setProperty("openkad.seed", ""+(i+basePort))
						.setProperty("openkad.net.udp.port", ""+(i+basePort)),
						
					new HttpConnectorModule()
						.setProperty("httpconnector.net.port", ""+(i+basePort)),
					
					new SimpleDHTModule()
						.setProperty("chunkeeper.dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5)),
						
					new ChunKeeperModule()
						.setProperty("chunkeeper.dht.storage.validtime", ""+TimeUnit.SECONDS.toMillis(10))
						.setProperty("chunkeeper.dht.storage.maxage", ""+TimeUnit.SECONDS.toMillis(15))
			);
			
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			
			HttpConnector httpConnector = injector.getInstance(HttpConnector.class);
			httpConnector.bind();
			httpConnector.start();
			
			ChunKeeper chunkeeper = injector.getInstance(ChunKeeper.class);
			chunkeeper.bind();
			chunkeepers.add(chunkeeper);
			
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		KeyFactory kf = kbrs.get(0).getKeyFactory();
		Key k1 = kf.generate();
		Key k2 = kf.generate();
		
		// storing data
		chunkeepers.get(0).store(k1, "abc");
		chunkeepers.get(5).store(k1, "edf");
		chunkeepers.get(7).store(k2, "hij");
		
		// wait for the data to propagate
		Thread.sleep(1000);
		
		// searching k1 data ("abc" and "edf")
		Set<Chunk> chunks = chunkeepers.get(2).findChunk(k1);
		Assert.assertEquals(2, chunks.size());
		
		
		List<Serializable> data = Lambda.extract(chunks, Lambda.on(Chunk.class).download());
		Assert.assertTrue(data.contains("abc"));
		Assert.assertTrue(data.contains("edf"));
		
		// searching k2 data ("hij")
		chunks = chunkeepers.get(9).findChunk(k2);
		Assert.assertEquals(1, chunks.size());
		
		data = Lambda.extract(chunks, Lambda.on(Chunk.class).download());
		Assert.assertTrue(data.contains("hij"));
	}
	
}
