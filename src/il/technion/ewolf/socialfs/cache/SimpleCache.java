package il.technion.ewolf.socialfs.cache;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.socialfs.Cache;
import il.technion.ewolf.socialfs.KeyHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;

public class SimpleCache<T extends KeyHolder> implements Cache<T> {

	private final Map<Key, T> cache = new ConcurrentHashMap<Key, T>();
	
	@Inject
	SimpleCache() {
		
	}
	
	@Override
	public void insert(T f) {
		cache.put(f.getKey(), f);
	}

	@Override
	public T search(Key fileKey) {
		return cache.get(fileKey);
	}

}
