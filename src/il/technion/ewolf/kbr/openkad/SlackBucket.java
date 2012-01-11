package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Node;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;

public class SlackBucket implements Bucket {

	
	private final List<KadNode> bucket;
	private final int maxSize;
	
	@Inject
	SlackBucket(int maxSize) {
		this.maxSize = maxSize;
		bucket = new LinkedList<KadNode>();
	}
	
	@Override
	public void insert(KadNode n) {
		// dont bother with other people wrong information
		if (n.hasNeverContacted())
			return;
		
		synchronized (bucket) {
			if (bucket.size() == maxSize)
				bucket.remove(0);
			
			bucket.add(n);
		}
	}

	@Override
	public void addNodesTo(Collection<Node> c) {
		synchronized (bucket) {
			for (KadNode n : bucket) {
				c.add(n.getNode());
			}
		}
	}

}
