package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DHTStorageTest {

	
	@Test
	public void test() throws Exception {
		Injector injector = Guice.createInjector(new KadNetModule());
		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		
		/*
		DHTStorage storage = new DHTStorage(1, 1, 1);
		
		Key key1 = kbr.getKeyFactory().generate();
		Key key2 = kbr.getKeyFactory().generate();
		storage.store(key1, Arrays.asList(new String[] {"aaa="}));
		storage.store(key1, Arrays.asList(new String[] {"bbb="}));
		storage.store(key2, Arrays.asList(new String[] {"ccc="}));
		
		Collection<String> vals = storage.get(key2);
		
		System.out.println(vals);
		*/
	}
}
