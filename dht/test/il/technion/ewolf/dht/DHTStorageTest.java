package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DHTStorageTest {

	private static class InserterThread extends Thread {
		
		private KeyFactory keyFactory;
		private Random rnd;
		private long runTime = 0;
		private DHTStorage storage;
		private List<Key> usedKeys = new ArrayList<Key>();
		
		private Map<Key, List<String>> inserted = new HashMap<Key, List<String>>();
		
		public InserterThread(KeyFactory keyFactory, long seed, DHTStorage storage) {
			this.keyFactory = keyFactory;
			this.rnd = new Random(seed);
			this.storage = storage;
		}
		
		public InserterThread setRunTime(long runTime) {
			this.runTime = runTime;
			return this;
		}
		
		public void run() {
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < runTime) {
				Key k = (!usedKeys.isEmpty() && rnd.nextBoolean()) ?
							usedKeys.get(rnd.nextInt(usedKeys.size())) :
							keyFactory.generate();
				
				if (!inserted.keySet().contains(k))
					usedKeys.add(k);
				
				String rndStr = Long.toString(rnd.nextLong());
				List<String> lst = inserted.get(k);
				if (lst == null) {
					lst = new ArrayList<String>();
					inserted.put(k, lst);
				}
				lst.add(rndStr);
				
				storage.store(k, Arrays.asList(new String[] {rndStr}));
			}
		}
		
		public Map<Key, List<String>> getInserted() {
			return inserted;
		}
		
	}
	@Test
	public void testBasic() throws Exception {
		Injector injector = Guice.createInjector(new KadNetModule());
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		
		injector = Guice.createInjector(new DHTModule(kbr));
		DHTStorage storage = injector.getInstance(DHTStorage.class);
		
		Key key1 = kbr.getKeyFactory().generate();
		Key key2 = kbr.getKeyFactory().generate();
		storage.store(key1, Arrays.asList(new String[] {"aaa="}));
		storage.store(key1, Arrays.asList(new String[] {"bbb="}));
		storage.store(key2, Arrays.asList(new String[] {"ccc="}));
		
		Assert.assertTrue(storage.get(key1).size() == 2);
		Assert.assertTrue(storage.get(key1).contains("aaa="));
		Assert.assertTrue(storage.get(key1).contains("bbb="));
		
		Assert.assertTrue(storage.get(key2).size() == 1);
		Assert.assertTrue(storage.get(key2).contains("ccc="));
		
	}
	
	
	@Test
	public void testConcurrency() throws Exception {
		Injector injector = Guice.createInjector(new KadNetModule());
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		
		injector = Guice.createInjector(new DHTModule(kbr));
		DHTStorage storage = injector.getInstance(DHTStorage.class);
		
		List<InserterThread> threads = new ArrayList<InserterThread>();
		for (int i=0; i < 5; ++i)
			threads.add(new InserterThread(kbr.getKeyFactory(), i, storage).setRunTime(10000));
		
		for (InserterThread t : threads)
			t.start();
		
		for (InserterThread t : threads)
			t.join();
				
		for (InserterThread t : threads) {
			
			for (Key k : t.getInserted().keySet()) {
				System.out.println(t.getInserted().get(k));
				Assert.assertTrue(storage.get(k).containsAll(t.getInserted().get(k)));
			}
		}
	}
}
