package dht.openkad.ops;

import java.io.IOException;
import java.util.List;

import dht.Key;
import dht.SerializerFactory;
import dht.openkad.KBucketsList;
import dht.openkad.KadConnection;
import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;
import dht.openkad.KadMsgBuilder;
import dht.openkad.KadNode;

public class StoreValueOperation extends KadOperation<Void> {

	private final KadMsg storeMsg;
	private final KadOperation<List<KadNode>> nodeLookupOp;
	
	public StoreValueOperation(KadNode localNode,
			KBucketsList kbuckets,
			KadOperationsExecutor executor,
			Key key,
			Object value,
			SerializerFactory serializer) throws IOException {
		
		super(localNode, kbuckets);
		this.storeMsg = new KadMsgBuilder()
			.setSrc(localNode)
			.setRpc(RPC.STORE)
			.setKey(key)
			.addValues(serializer, value)
			.buildMessage();
		nodeLookupOp = executor.createNodeLookupOperation(key);
	}

	
	
	@Override
	public Void call() throws Exception {
		List<KadNode> kClosestNodes = nodeLookupOp.call();
		for (KadNode n : kClosestNodes) {
			KadConnection conn = null;
			try {
				conn = n.openConnection(); 
				conn.sendMessage(this.storeMsg);
			} catch (IOException e) {
				System.err.println("unable to send store msg to "+n);
			} finally {
				if (conn != null)
					conn.close();
			}
		}
		return null;
	}

}
