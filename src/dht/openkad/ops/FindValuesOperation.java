package dht.openkad.ops;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dht.Key;
import dht.SerializerFactory;
import dht.openkad.KBucketsList;
import dht.openkad.KadConnection;
import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;
import dht.openkad.KadMsgBuilder;
import dht.openkad.KadNode;
import dht.openkad.validator.KadBasicMsgValidator;
import dht.openkad.validator.KadMsgValidator;
import dht.openkad.validator.KadRPCValidator;

public class FindValuesOperation extends KadOperation<Set<Object>> {

	private final KadMsg findMsg;
	private final KadOperation<List<KadNode>> nodeLookupOp;
	private final KadMsgValidator basicValidator;
	private final SerializerFactory serializer;
	
	public FindValuesOperation(
			KadNode localNode,
			KBucketsList kbuckets,
			KadOperationsExecutor executor,
			Key key,
			SerializerFactory serializer,
			KadBasicMsgValidator validator) throws IOException {
		
		super(localNode, kbuckets);
		this.findMsg = new KadMsgBuilder()
			.setSrc(localNode)
			.setRpc(RPC.FIND_VALUE)
			.setKey(key)
			.buildMessage();
		this.serializer = serializer;
		nodeLookupOp = executor.createNodeLookupOperation(key);
		this.basicValidator = validator;
	}

	
	
	private List<KadConnection> sendFindValue(List<KadNode> kClosestNodes) {
		List<KadConnection> $ = new ArrayList<KadConnection>();
		for (KadNode n : kClosestNodes) {
			try {
				KadConnection conn = n.openConnection();
				conn.sendMessage(findMsg);
				$.add(conn);
			} catch (IOException e) {
				System.err.println("unable to send store msg to "+n);
			}
		}
		return $;
	}
	
	@Override
	public Set<Object> call() {
		List<KadNode> kClosestNodes;
		try {
			kClosestNodes = nodeLookupOp.call();
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError("Should never happen");
		}
		List<KadConnection> connections = sendFindValue(kClosestNodes);
		
		Set<Object> $ = new HashSet<Object>();
		for (KadConnection conn : connections) {
			try {
				KadMsg findValResponse = conn.recvMessage(
						basicValidator,
						KadRPCValidator.findValueValidator);
				// de-serialize returned objects 
				for (byte[] val : findValResponse.getValues()) {
					ObjectInput objInput = serializer.createObjectInput(new ByteArrayInputStream(val));
					$.add(objInput.readObject());
					objInput.close();
				}
			} catch (IOException e) {
				System.err.println("unable to recv store msg to "+conn);
			} catch (ClassNotFoundException e) {
				System.err.println("unable to recv store msg to "+conn);
			} finally {
				conn.close();
			}
		}
		return $;
	}

}
