package dht.openkad.ops;

import java.util.Collection;

import dht.openkad.KBucketsList;
import dht.openkad.KadNode;

class InsertNodeIfNotFullOperation extends KadOperation<Void> {

	private final Collection<KadNode> nodesToInsert;
	
	InsertNodeIfNotFullOperation(KadNode localNode, KBucketsList kbuckets, Collection<KadNode> nodesToInsert) {
		super(localNode, kbuckets);
		this.nodesToInsert = nodesToInsert;
	}

	@Override
	public Void call() {
		for (KadNode n : nodesToInsert) {
			if (getLocalNode().equals(n))
				continue;
			getKbucketsList().insertNodeIfNotFull(n);
		}
		return null;
	}

}
