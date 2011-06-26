package dht.openkad;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import dht.Key;
import dht.KeyFactory;
import dht.openkad.ops.KadOperationsExecutor;
import dht.openkad.validator.KadBasicMsgValidator;

public class KBucketsList {

	private final KBucket[] kbuckets;
	private final KadNode localNode;
	private final int bucketSize;
	private final KeyFactory keyFactory;
	
	@Inject
	KBucketsList(
			@Named("kad.bucketsize") int bucketSize,
			@Named("kad.localnode") KadNode localNode,
			KeyFactory keyFactory) {
		
		this.localNode = localNode;
		this.bucketSize = bucketSize;
		this.keyFactory = keyFactory;
		KadBasicMsgValidator basicValidator = new KadBasicMsgValidator(keyFactory);
		kbuckets = new KBucket[keyFactory.getBitCount()];
		for (int i=0; i < kbuckets.length; kbuckets[i++] = new KBucket(bucketSize, localNode, basicValidator));
	}
	
	public int getBucketSize() {
		return bucketSize;
	}
	public int getNrBuckets() {
		return kbuckets.length;
	}
	private int getKBucketIndex(Key key) {
		byte[] b1 = localNode.getKey().getBytes();
		byte[] b2 = key.getBytes();
		
		if (b1.length * 8 != kbuckets.length ||
			b2.length * 8 != kbuckets.length) {
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
	public List<KadNode> getKClosestNodes(Key key) {
		return getKClosestNodes(key, bucketSize);
	}
	public List<KadNode> getKClosestNodes(Key key, int k) {
		List<KadNode> $ = new ArrayList<KadNode>();
		
		int index = getKBucketIndex(key);
		if (index > 0)
			$.addAll(kbuckets[index].getNodes());
		
		if ($.size() < k) {
			// look in other buckets
			for (int i=1; $.size() < k; ++i) {
				if (index + i < kbuckets.length)
					$.addAll(kbuckets[index+i].getNodes());
				if (index - i >= 0)
					$.addAll(kbuckets[index-i].getNodes());
				
				if (kbuckets.length <= index + i && index - i < 0)
					break;
			}
		}
		$.add(localNode);
		Collections.sort($, new KadNodeComparator(key));
		for (int j=$.size()-1; j >= k; $.remove(j--));
		
		return $;
	}
	
	public void insertNode(KadNode s) {
		kbuckets[getKBucketIndex(s.getKey())].insertNode(s);
	}
	
	public void insertNodeIfNotFull(KadNode s) {
		kbuckets[getKBucketIndex(s.getKey())].insertNodeIfNotFull(s);
	}
	
	public void refreshBucket(int i, KadOperationsExecutor opExecutor) {
		
		BigInteger minSize = new BigInteger("2").pow(i);
		BigInteger maxSize = new BigInteger("2").pow(i+1);
		BigInteger rnd = keyFactory.generate(minSize, maxSize).getInt();
		BigInteger local = localNode.getKey().getInt();
		// FIXME: im not sure about the random min max size
		
		Key bucketRandomKey = keyFactory.getFromInt(local.xor(rnd));
		/*
		System.out.println("\nbucket num: "+i);
		System.out.println("rnd: "+rnd.toString(2));
		System.out.println("local: "+localNode.getKey().getInt().toString(2));
		System.out.println("randm: "+bucketRandomKey.getInt().toString(2));
		*/
		try {
			opExecutor.createNodeLookupOperation(bucketRandomKey).call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public String toString() {
		String $ = "";
		for (int i=0; i < kbuckets.length; ++i) {
			$ += String.format("%3d: ", i);
			$ += kbuckets[i].toString();
			$ += "\n";
		}
		return $;
	}
	
	// FOR TESTING ONLY
	KBucket[] getKBuckets() {
		return kbuckets;
	}
}
