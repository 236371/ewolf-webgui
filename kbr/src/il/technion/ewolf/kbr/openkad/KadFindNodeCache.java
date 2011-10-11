package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Future;

import com.google.inject.Inject;
import com.google.inject.name.Named;

class KadFindNodeCache {

	private final Queue<CacheEntry> entries = new PriorityQueue<CacheEntry>();
	private final Map<Key, CacheEntry> entryFromKey = new HashMap<Key, CacheEntry>();
	
	private class CacheEntry implements Comparable<CacheEntry>{
		private final Key key;
		private final long insertedTime;
		private final int nodeCount;
		private final Future<List<KadNode>> nodes;
		
		CacheEntry(
				Key key,
				int nodeCount,
				Future<List<KadNode>> nodes) {
			
			this.key = key;
			this.insertedTime = System.currentTimeMillis();
			this.nodeCount = nodeCount;
			this.nodes = nodes;
		}

		public boolean isValid() {
			return System.currentTimeMillis() - insertedTime < maxTime;
		}
		
		public int getNodeCount() {
			return nodeCount;
		}

		public Future<List<KadNode>> getNodes() {
			return nodes;
		}

		public Key getKey() {
			return key;
		}

		@Override
		public int compareTo(CacheEntry o) {
			return ((Long)insertedTime).compareTo(o.insertedTime);
		}
		
		@Override
		public String toString() {
			return Long.toString(insertedTime);//key.toString();
		}
	}
	
	private final int maxEntries;
	private final long maxTime;
	
	@Inject
	KadFindNodeCache(
			@Named("kadnet.cache.maxentries") int maxEntries,
			@Named("kadnet.cache.maxtime") long maxTime) {
		
		this.maxEntries = maxEntries;
		this.maxTime = maxTime;
		
	}
	
	private void removeInvalidEntries() {
		while (entries.peek() != null && !entries.peek().isValid())
			entryFromKey.remove(entries.poll().getKey());
	}
	
	private void addEntry(Key key, CacheEntry e) {
		entries.offer(e);
		entryFromKey.put(key, e);
	}
	
	public synchronized void put(Key key, Future<List<KadNode>> nodes, int nodeCount) {
		removeInvalidEntries();
			
		entries.remove(entryFromKey.get(key));
		
		if (entries.size() == maxEntries && !entries.isEmpty())
			entries.poll();
		
		if (entries.size() < maxEntries)
			addEntry(key, new CacheEntry(key, nodeCount, nodes));
	}
	
	public synchronized Future<List<KadNode>> search(Key key, int minNodeCount) {
		removeInvalidEntries();
		
		CacheEntry e = entryFromKey.get(key);
		if (e == null || e.getNodeCount() < minNodeCount)
			return null;
		return e.getNodes();
	}
	
	
}
