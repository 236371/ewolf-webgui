package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import ch.lambdaj.Lambda;
import ch.lambdaj.function.convert.ConstructorArgumentConverter;

import com.google.inject.Inject;

class DHTStorage {

	private Map<Key, List<ByteArray>> storage = new HashMap<Key, List<ByteArray>>();
	
	
	@Inject
	DHTStorage(
			@Named("dht.storage.maxlifetime") long maxLifetime,
			@Named("dht.storage.maxentrysize") int maxEntrySize,
			@Named("dht.storage.maxentries") int maxEntries) {
		
		
	}
	
	public void store(Key key, Collection<byte[]> val) {
	}
	
	public List<byte[]> get(Key key) {
		return null;
	}
	
}
