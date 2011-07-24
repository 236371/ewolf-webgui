package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadNode;

import java.util.Collection;


public class InsertNodeIfNotFullOperation extends KadOperation<Void> {

	private final KBuckets kbuckets;
	private final Collection<KadNode> nodes;
	
	InsertNodeIfNotFullOperation(KBuckets kbuckets, Collection<KadNode> nodes) {
		this.kbuckets = kbuckets;
		this.nodes = nodes;
	}
	
	@Override
	public Void call() throws Exception {
		
		for (KadNode s : nodes) {
			kbuckets.insertIfNotFull(s);
		}
		
		return null;
	}

	
	
}
