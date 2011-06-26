package dht.openkad.ops;

import java.util.concurrent.Callable;

import dht.openkad.KBucketsList;
import dht.openkad.KadNode;

public abstract class KadOperation<V> implements Callable<V> {

	private final KadNode localNode;
	private final KBucketsList kbuckets;
	
	
	public KadOperation(KadNode localNode, KBucketsList kbuckets) {
		this.localNode = localNode;
		this.kbuckets = kbuckets;
	}
	
	


	KadNode getLocalNode() {
		return localNode;
	}


	KBucketsList getKbucketsList() {
		return kbuckets;
	}

	
	
}
