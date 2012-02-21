package il.technion.ewolf.dht.storage;

import il.technion.ewolf.dht.DHTStorage;
import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;

/**
 * Simple volatile storage that never removes old data
 *  
 * @author eyal.kibbar@gmail.com
 *
 */
public class SimpleDHTStorage implements DHTStorage {

	private final Map<Key, Set<Serializable>> storage;
	
	
	@Inject
	SimpleDHTStorage() {
		storage = new ConcurrentHashMap<Key, Set<Serializable>>();
	}


	@Override
	public void store(Key key, long age, Serializable data) {
		Set<Serializable> s = null;
		synchronized (storage) {
			 s = storage.get(key);
			 if (s == null) {
				 s = new HashSet<Serializable>();
				 storage.put(key, s);
			 }
		}
		
		synchronized (s) {
			s.add(data);
		}
	}


	@Override
	public Set<Serializable> search(Key key) {
		Set<Serializable> res = storage.get(key);
		if (res == null)
			return Collections.emptySet();

		synchronized (res) {
			return new HashSet<Serializable>(res);
		}
	}


	@Override
	public void setDHTName(String name) {
		// nothing to do
	}
}
