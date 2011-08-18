package il.technion.ewolf.dht;

import java.util.Arrays;

import org.junit.Test;

public class DHTStorageTest {

	
	@Test
	public void test() throws Exception {
		DHTStorage storage = new DHTStorage(1, 1, 1);
		
		storage.store(null, Arrays.asList(new byte[] {1,2}, new byte[] {3,4}));
	}
}
