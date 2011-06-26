package dht.openkad;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import dht.Key;

public class KadMsg implements Serializable {

	private static final long serialVersionUID = 4948097143303755735L;


	public static enum RPC {
		PING,
		STORE,
		FIND_NODE,
		FIND_VALUE
	}
	
	private final RPC rpc;
	private final KadNode src;
	private final Key key;
	private final KadNode[] knownClosestNodes;
	private final List<byte[]> values;
	
	
	public KadMsg(
			RPC rpc,
			KadNode src,
			Key key,
			KadNode[] knownClosestNodes,
			List<byte[]> values) {
		this.rpc = rpc;
		this.src = src;
		this.key = key;
		this.knownClosestNodes = knownClosestNodes;
		this.values = values;
	}


	public RPC getRpc() {
		return rpc;
	}


	public KadNode getSrc() {
		return src;
	}


	public Key getKey() {
		return key;
	}


	public List<KadNode> getKnownClosestNodes() {
		return Arrays.asList(knownClosestNodes);
	}


	public  List<byte[]> getValues() {
		return values;
	}
	
	
	
}
