package dht.openkad;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

import dht.Key;

class LocalStorage {

	
	private Map<Key, Set<byte[]>> values = new HashMap<Key, Set<byte[]>>();
	
	@Inject
	LocalStorage() {
		
	}
	
	public synchronized void put(Key key, byte[] data) {
		Set<byte[]> $ = values.get(key);
		if ($ == null) {
			$ = Collections.synchronizedSet(new HashSet<byte[]>());
			values.put(key, $);
		}
		$.add(data);
	}
	
	public synchronized void putAll(Key key, Collection<byte[]> data) {
		Set<byte[]> $ = values.get(key);
		if ($ == null) {
			$ = Collections.synchronizedSet(new HashSet<byte[]>());
			values.put(key, $);
		}
		$.addAll(data);
	}
	
	public synchronized Set<byte[]> get(Key key) {
		Set<byte[]> $ = values.get(key);
		if ($ == null)
			return Collections.emptySet();
		return $;
	}
}
