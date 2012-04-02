package il.technion.ewolf.dht;

import static junit.framework.Assert.assertEquals;
import il.technion.ewolf.dht.storage.AgeLimitedDHTStorage;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class DHTest {
	private static final int BASE_PORT = 10000;
	private List<Injector> injectors = new LinkedList<Injector>();

	@After
	public void cleanup() {
		for (Injector inj: injectors) {
			inj.getInstance(KeybasedRouting.class).shutdown();
		}
		injectors.clear();
	}
	
	@Test
	public void itShouldStoreData() throws Exception {
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		List<DHT> dhts = new ArrayList<DHT>();
		for (int i=0; i < 16; ++i) {
			Injector injector = Guice.createInjector(
					new KadNetModule()
						.setProperty("openkad.keyfactory.keysize", "2")
						.setProperty("openkad.bucket.kbuckets.maxsize", "5")
						.setProperty("openkad.seed", ""+(i+BASE_PORT))
						.setProperty("openkad.net.udp.port", ""+(i+BASE_PORT)),
						
					new SimpleDHTModule()
						.setProperty("dht.storage.checkInterval", ""+TimeUnit.SECONDS.toMillis(5))
			);
			injectors.add(injector);
			
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
			
			DHTStorage storage = injector.getInstance(AgeLimitedDHTStorage.class)
				.setMaxAge(TimeUnit.SECONDS.toMillis(15))
				.setRereplicate(true)
				.setValidTime(TimeUnit.SECONDS.toMillis(10))
				.create();
					
			DHT dht = injector.getInstance(DHT.class)
				.setName("dht")
				.setStorage(storage)
				.create();
			
			dhts.add(dht);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = BASE_PORT + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
		
		
		dhts.get(0).put("abc", "a", "b", "c");
		dhts.get(7).put("edf", "e");
		
		Thread.sleep(11000);
		
		// check existing entries are re-replicated
		for (int i=0; i < kbrs.size(); ++i) {
		
			List<Serializable> res = dhts.get(i).get("a", "b", "c");
			
			assertEquals(1, res.size());
			assertEquals("abc", res.get(0));
			
			
			res = dhts.get(i).get("e");
			
			assertEquals(1, res.size());
			assertEquals("edf", res.get(0));
		}
		
		// check old entries are removed
		Thread.sleep(11000);
		
		for (int i=0; i < kbrs.size(); ++i) {
			
			List<Serializable> res = dhts.get(i).get("a", "b", "c");
			
			assertEquals(0, res.size());
			
			res = dhts.get(i).get("e");
			
			assertEquals(0, res.size());
		}
		
	}
}