package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.List;


public class KadMessage implements Serializable {

	private static final long serialVersionUID = 7311694650346299141L;

	public enum RPC {
		PING,
		FIND_NODE,
		MSG,
		CONN,
		ACK,
	};
	
	private final RPC rpc;
	
	private final List<KadNode> path;
	private final Key dst;
	private final List<Key> contacts;
	private final boolean keepAlive;
	
	// FIND_NODE params
	private final Key key;
	private final List<KadNode> nodes;
	private final int maxNodeCount;
	
	private final String tag;
	
	// MSG params
	private final byte[] content;
	
	// CONN params
	private final int connPort;
	
	public KadMessage(
			RPC rpc,
			String tag,
			Key key,
			List<Key> contacts,
			List<KadNode> path,
			Key dst,
			List<KadNode> nodes, 
			int maxNodeCount,
			byte[] content,
			int connPort,
			boolean keepAlive) {
		
		this.rpc = rpc;
		this.tag = tag;
		this.key = key;
		this.contacts = contacts;
		this.dst = dst;
		this.nodes = nodes;
		this.maxNodeCount = maxNodeCount;
		this.path = path;
		this.content = content;
		this.connPort = connPort;
		this.keepAlive = keepAlive;
	}

	public int getMaxNodeCount() {
		return maxNodeCount;
	}
	public Key getKey() {
		return key;
	}
	public RPC getRpc() {
		return rpc;
	}

	public String getTag() {
		return tag;
	}
	
	public List<KadNode> getPath() {
		return path;
	}
	
	public Key getDst() {
		return dst;
	}

	public synchronized List<KadNode> getNodes() {
		return nodes;
	}
	
	public byte[] getConent() {
		return content;
	}
	
	public KadNode getLastHop() {
		return getPath().get(getPath().size()-1);
	}
	
	public KadNode getFirstHop() {
		return getPath().get(0);
	}
	
	
	public int getConnPort() {
		return connPort;
	}
	
	public boolean isKeepAlive() {
		return keepAlive;
	}

	public List<Key> getContacts() {
		return contacts;
	}
}
