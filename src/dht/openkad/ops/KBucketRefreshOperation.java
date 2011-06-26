package dht.openkad.ops;

import dht.openkad.KBucketsList;
import dht.openkad.KadNode;

class KBucketRefreshOperation extends KadOperation<Void> {

	private final int bucketIndex;
	private final KadOperationsExecutor opExecutor;
	
	public KBucketRefreshOperation(KadNode localNode, KBucketsList kbuckets, KadOperationsExecutor opExecutor, int bucketIndex) {
		super(localNode, kbuckets);
		this.bucketIndex = bucketIndex;
		this.opExecutor = opExecutor;
	}

	@Override
	public Void call() {
		getKbucketsList().refreshBucket(bucketIndex, opExecutor);
		return null;
	}

	
	
}
