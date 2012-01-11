package il.technion.ewolf.kbr.openkad.cache;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

public class OptimalKadCache implements KadCache {

	private final long validTime;
	private final AtomicInteger optimalCacheMaxSize;
	
	protected class CacheEntry {
		private final List<Node> nodes;
		private final long timestamp;
		private final Key key;
		
		CacheEntry(List<Node> nodes, Key key) {
			this.nodes = nodes;
			this.key = key;
			this.timestamp = System.currentTimeMillis();
		}
		
		public List<Node> getNodes() {
			return nodes;
		}
		
		public boolean isValid() {
			return timestamp + validTime > System.currentTimeMillis(); 
		}
		
		public Key getKey() {
			return key;
		}
	}
	
	protected final Map<Key, CacheEntry> cache = new HashMap<Key, CacheEntry>();
	
	@Inject
	OptimalKadCache(
			@Named("openkad.cache.validtime") long validTime,
			@Named("openkad.testing.optimalCacheMaxSize") AtomicInteger optimalCacheMaxSize) {
		this.validTime = validTime;
		this.optimalCacheMaxSize = optimalCacheMaxSize;
	}
	
	
	@Override
	public synchronized void insert(Key key, List<Node> nodes) {
		//System.out.println(localNode.getKey()+": inserting "+key+" => "+nodes);
		cache.put(key, new CacheEntry(nodes, key));
		
		if (optimalCacheMaxSize.get() < cache.size()) {
			optimalCacheMaxSize.set(cache.size());
		}
	}

	@Override
	public synchronized List<Node> search(Key key) {
		CacheEntry cacheEntry = searchCacheEntry(key);
		if (cacheEntry == null)
			return null;
		
		if (!cacheEntry.isValid()) {
			remove(cacheEntry);
			return null;
		}
		
		//System.out.println(localNode.getKey()+": Cache hit !");
		
		return cacheEntry.getNodes();
	}
	
	protected void remove(CacheEntry entry) {
		cache.remove(entry.getKey());
	}

	protected CacheEntry searchCacheEntry(Key key) {
		return cache.get(key);
	}
	
	protected void insertCacheEntry(CacheEntry c) {
		cache.put(c.getKey(), c);
	}
	
	@Override
	public void clear() {
		cache.clear();
	}
}
