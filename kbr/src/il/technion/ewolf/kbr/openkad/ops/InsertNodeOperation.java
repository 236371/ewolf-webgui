package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadNode;

import java.util.logging.Logger;

public class InsertNodeOperation extends KadOperation<Void> {

	private final KBuckets kbuckets;
	private final KadNode[] nodes;
	
	InsertNodeOperation(Logger logger, KBuckets kbuckets, KadNode ... nodes) {
		super(logger);
		this.kbuckets = kbuckets;
		this.nodes = nodes;
	}
	
	@Override
	public Void call() throws Exception {
		
		for (KadNode s : nodes) {
			logger.info("trying to insert node "+s.getKey());
			if (kbuckets.insert(s))
				logger.info("node "+s.getKey()+" inserted to kbuckets");
		}
		
		return null;
	}

	
	
}
