package dht.openkad.validator;

import dht.KeyFactory;
import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;
import dht.openkad.KadNode;


public class KadBasicMsgValidator implements KadMsgValidator {

	private final KeyFactory keyFactory;
	
	public KadBasicMsgValidator(KeyFactory keyFactory) {
		this.keyFactory = keyFactory;
	}
	
	@Override
	public void validate(KadMsg msg) {
		
		if (msg.getRpc() == null)
			throw new IllegalArgumentException("missing rpc");
		if (msg.getSrc() == null)
			throw new IllegalArgumentException("missing src");
		
		if (!msg.getSrc().validate(keyFactory))
			throw new IllegalArgumentException("invalid src: "+msg.getSrc());
		
		if (msg.getRpc().equals(RPC.PING))
			return;
		
		
		if (msg.getKey() == null)
			throw new IllegalArgumentException("missing key");
		if (msg.getKey().getBytes().length != keyFactory.getByteCount())
			throw new IllegalArgumentException("wrong key length");
		
		switch(msg.getRpc()) {
			
		case FIND_NODE:
			if (msg.getKnownClosestNodes() == null)
				throw new IllegalArgumentException("missing known closest nodes");
			for (KadNode n : msg.getKnownClosestNodes()) {
				if (!n.validate(keyFactory))
					throw new IllegalArgumentException("invalid nodes: "+n);
			}
			break;
		case FIND_VALUE:
			if (msg.getKnownClosestNodes() == null)
				throw new IllegalArgumentException("missing known closest nodes");
			if (msg.getValues() == null)
				throw new IllegalArgumentException("missing values");
			for (KadNode n : msg.getKnownClosestNodes()) {
				if (!n.validate(keyFactory))
					throw new IllegalArgumentException("invalid nodes: "+n);
			}
			break;
		case STORE:
			if (msg.getValues() == null || msg.getValues().isEmpty())
				throw new IllegalArgumentException("missing values");
		}
	}

}
