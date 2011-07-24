package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadNode;

public class InsertNodeOperation extends KadOperation<Void> {

	private final KBuckets kbuckets;
	private final KadNode[] nodes;
	
	InsertNodeOperation(KBuckets kbuckets, KadNode ... nodes) {
		this.kbuckets = kbuckets;
		this.nodes = nodes;
	}
	
	@Override
	public Void call() throws Exception {
		
		for (KadNode s : nodes) {
			kbuckets.insert(s);
		}
		
		return null;
	}

	
	
}
