package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadNode;

import java.util.Collection;
import java.util.logging.Logger;


public class InsertNodeIfNotFullOperation extends KadOperation<Void> {

	private final KBuckets kbuckets;
	private final Collection<KadNode> nodes;
	
	InsertNodeIfNotFullOperation(Logger logger, KBuckets kbuckets, Collection<KadNode> nodes) {
		super(logger);
		this.kbuckets = kbuckets;
		this.nodes = nodes;
	}
	
	@Override
	public Void call() throws Exception {
		
		for (KadNode s : nodes) {
			logger.info("trying to insert node "+s.getKey());
			if (kbuckets.insertIfNotFull(s))
				logger.info("node "+s.getKey()+" inserted to kbuckets");
		}
		
		return null;
	}

	
	
}
