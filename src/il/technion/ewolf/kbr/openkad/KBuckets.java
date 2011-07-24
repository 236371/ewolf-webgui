package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeyHolder;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


import com.google.inject.Inject;
import com.google.inject.name.Named;


public class KBuckets {
	private final KeyFactory keyFactory;
	private final List<List<KadNode>> kbuckets;
	private final KadNode localNode;
	private final int bucketSize;
	
	@Inject
	KBuckets(
			@Named("kadnet.bucketsize") int bucketSize,
			@Named("kadnet.localnode") KadNode localNode,
			KeyFactory keyFactory) {
		
		this.keyFactory = keyFactory;
		this.localNode = localNode;
		this.bucketSize = bucketSize;
		
		kbuckets = new ArrayList<List<KadNode>>();
		for (int i=0; i < keyFactory.getBitCount(); ++i) {
			kbuckets.add(new LinkedList<KadNode>());
		}

	}
	
	private int getKBucketIndex(Key key) {
		byte[] b1 = localNode.getKey().getBytes();
		byte[] b2 = key.getBytes();
		
		if (b1.length * 8 != kbuckets.size() ||
			b2.length * 8 != kbuckets.size()) {
			throw new IllegalArgumentException("wrong key: length is incompatable: "+key);
		}
		
		for (int i=0; i < b1.length; ++i) {
			byte xor = (byte)(b1[i] ^ b2[i]);
			if (xor == 0) continue;
			// found different byte
			// find first different bit
			int j;
			for (j=7; (xor & (1 << j)) == 0; --j);
	
			return (b1.length-i-1)*8+j;
		}
		return -1;
	}
	
	private List<KadNode> getBucket(Key k) {
		int i = getKBucketIndex(k);
		if (i < 0)
			return null;
		return kbuckets.get(i);
	}
	
	
	
	private boolean ping(KadNode x) {
		for (KadConnection conn : x.getKadConnections()) {
			try {
				conn = x.getKadConnections().get(0);
				
				new KadMessageBuilder()
					.addHop(KBuckets.this.localNode)
					.setRpc(RPC.PING)
					.setDst(x.getKey())
					.sendTo(conn);
				
				// block until recved message
				conn.recvMessage();
				return true;
			} catch (Exception e) {
				
			} finally {
				if (conn != null)
					conn.close();
			}
		}
		return false;
	}
	
	public boolean insert(KadNode s) {
		List<KadNode> bucket = getBucket(s.getKey());
		if (bucket == null)
			return false;
		
		KadNode x = null;
		
		synchronized(bucket) {
			int i = bucket.indexOf(s);
			if (i != -1) {
				x = bucket.remove(i);
				//x.mergeProxies(s);
				// push x to the back
				bucket.add(x);
				return true;
				
			}
			// s is not in bucket
			if (bucket.size() < bucketSize) { // has room for s
				bucket.add(s);
				return true;
			}
		}
		
		synchronized(bucket) {
			if (bucket.isEmpty()) {
				bucket.add(s);
				return true;
			}
			x = bucket.get(0);
		}
		
		if (ping(x)) { // PING WAS RECVED
			synchronized(bucket) {
				if (bucket.remove(x)) {
					bucket.add(x);
					return false;
				}
			}
		} else { // PING WAS NOT RECVED
			synchronized(bucket) {
				if (bucket.remove(x)) {
					bucket.add(s);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean insertIfNotFull(KadNode s) {
		List<KadNode> bucket = getBucket(s.getKey());
		if (bucket == null)
			return false;
		
		synchronized(bucket) {
			int i = bucket.indexOf(s);
			if (i != -1) {
				// s already in bucket
				//bucket.get(i).mergeProxies(s);
			} else {
				if (bucket.size() < bucketSize) {
					// still has place and s is not inside
					bucket.add(s);
					return true;
				}
			}
		}
		return false;
		
	}
	
	public List<KadNode> getKClosestNodes(Key key) {
		return getKClosestNodes(key, new HashSet<KadNode>());
	}
			
	public List<KadNode> getKClosestNodes(Key key, Collection<? extends KeyHolder> exclude) {
		return getKClosestNodes(key, exclude, bucketSize);
	}
	public List<KadNode> getKClosestNodes(Key key, Collection<? extends KeyHolder> exclude, int k) {
		if (!keyFactory.isValid(key) || k <= 0)
			throw new IllegalArgumentException();
		
		List<KadNode> $ = new ArrayList<KadNode>();
		
		
		List<KadNode> bucket = getBucket(key);
		if (bucket != null) {
			synchronized (bucket) {
				$.addAll(bucket);
			}
		}
		
		int index = getKBucketIndex(key);
		if ($.size() < k) {
			// look in other buckets
			for (int i=1; $.size() < k; ++i) {
				try {
					bucket = kbuckets.get(index + i);
					synchronized (bucket) {
						$.addAll(bucket);
					}
				} catch (Exception e) {}
				try {
					bucket = kbuckets.get(index - i);
					synchronized (bucket) {
						$.addAll(bucket);
					}
				} catch (Exception e) {}
				
				Iterator<KadNode> itr = $.iterator();
				while (itr.hasNext()) {
					if (exclude.contains(itr.next().getKey()))
						itr.remove();
				}
				
				if (kbuckets.size() <= index + i && index - i < 0)
					break;
				
				
			}
		}
		Collections.sort($, new KadKeyComparator(key));
		for (int j=$.size()-1; j >= k; $.remove(j--));
		
		return $;
	}
	
	
	
	public int getBucketSize() {
		return bucketSize;
	}
	public int getNrBuckets() {
		return kbuckets.size();
	}

	public String toString() {
		String $ = "";
		for (int i=0; i < kbuckets.size(); ++i) {
			$ += i+": "+kbuckets.get(i)+"\n";
		}
		return $;
	}


	public Set<KadNode> getAllNodes() {
		Set<KadNode> $ = new HashSet<KadNode>();
		for (int i=0; i < kbuckets.size(); ++i) {
			List<KadNode> bucket = kbuckets.get(i);
			synchronized (bucket) {
				$.addAll(bucket);
			}
		}
		return $;
	}
	
	public boolean addProxyEndpoint(KadNode s) {
		List<KadNode> bucket = getBucket(s.getKey());
		if (bucket == null)
			return false;
		s.addProxy(localNode);
		return insert(s);
	}
	
}
