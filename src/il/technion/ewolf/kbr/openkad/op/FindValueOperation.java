package il.technion.ewolf.kbr.openkad.op;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

import java.util.List;
import java.util.concurrent.Callable;

public abstract class FindValueOperation implements Callable<List<Node>> {

	protected Key key;
	protected int maxNodes;
	
	private final int kBucketSize;
	
	protected FindValueOperation(int kBucketSize) {
		this.kBucketSize = kBucketSize;
	}
	
	public FindValueOperation setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public FindValueOperation setMaxNodes(int maxNodes) {
		this.maxNodes = Math.max(maxNodes, kBucketSize);
		return this;
	}
	
	
	public abstract int getNrQueried();
	
}
