package dht.openkad.ops;

import dht.openkad.KBucketsList;
import dht.openkad.KadNode;

public class JoinOperation extends KadOperation<Void> {

	private final KadOperationsExecutor executor;
	private final KadNode bootstrapNode;

	public JoinOperation(KadNode localNode, KBucketsList kbuckets, KadOperationsExecutor executor, KadNode bootstrapNode) {
		super(localNode, kbuckets);
		this.executor = executor;
		this.bootstrapNode = bootstrapNode;
	}

	@Override
	public Void call() throws Exception {
		executor.createInsertNodeOperation(bootstrapNode).call();
		executor.createNodeLookupOperation(getLocalNode().getKey()).call();
		for (int i=0; i < getKbucketsList().getNrBuckets(); executor.createRefreshBucket(i++).call());
		return null;
	}

}
