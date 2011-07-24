package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.openkad.KadNode;

import java.math.BigInteger;


class BucketRefreshOperation extends KadOperation<Void> {

	private final KadNode localNode;
	private final KadOperationsExecutor opExecutor;
	private final KeyFactory keyFactory;
	private final int bucketSize;
	private final int bucketNum;
	
	BucketRefreshOperation(
			KadNode localNode,
			KadOperationsExecutor opExecutor,
			KeyFactory keyFactory,
			int bucketSize,
			int bucketNum) {
		
		this.localNode = localNode;
		this.opExecutor = opExecutor;
		this.keyFactory = keyFactory;
		this.bucketSize = bucketSize;
		this.bucketNum = bucketNum;
	}
	
	@Override
	public Void call() throws Exception {
		
		Key rndKey = keyFactory.generate(
				new BigInteger("2").pow(bucketNum),
				new BigInteger("2").pow(bucketNum+1));
		
		Key lookupKey = keyFactory.getFromInt(localNode.getKey().getInt().xor(rndKey.getInt()));
		
		opExecutor.createNodeLookupOperation(lookupKey, bucketSize).call();
		
		return null;
	}

}
