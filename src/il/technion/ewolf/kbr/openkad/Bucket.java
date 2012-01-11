package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Node;

import java.util.Collection;

public interface Bucket {

	public void insert(KadNode n);

	void addNodesTo(Collection<Node> c);
}
