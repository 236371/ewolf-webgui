package dht.openkad.validator;

import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;

public class KadRPCValidator implements KadMsgValidator {

	
	public final static KadMsgValidator pingValidator = new KadRPCValidator(RPC.PING);
	public final static KadMsgValidator storeValidator = new KadRPCValidator(RPC.STORE);
	public final static KadMsgValidator findNodeValidator = new KadRPCValidator(RPC.FIND_NODE);
	public final static KadMsgValidator findValueValidator = new KadRPCValidator(RPC.FIND_VALUE);
	
	private final RPC rpc;
	
	KadRPCValidator(RPC rpc) {
		this.rpc = rpc;
	}
	
	@Override
	public void validate(KadMsg msg) {
		if (!rpc.equals(msg.getRpc()))
			throw new IllegalArgumentException("wrong rpc, expecting "+rpc+" but got "+msg.getRpc());
	}

}
