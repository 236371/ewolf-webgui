package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Node;

import java.util.Collection;

/**
 * A default stupid implementation of Bucket that does nothing
 * 
 * @author eyal.kibbar@gmail.com
 */
class DummyBucket implements Bucket {

	@Override
	public void insert(KadNode n) {
	}

	@Override
	public void addNodesTo(Collection<Node> c) {
	}

	@Override
	public void markDead(Node n) {
	}
}
