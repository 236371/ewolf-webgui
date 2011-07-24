package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class KadMessageBuilder {

	private RPC rpc = null;
	private String tag = null;
	private Key key = null;
	private List<Key> contacts = new ArrayList<Key>();
	private Key dst = null;
	private List<KadNode> path = new ArrayList<KadNode>();
	private List<KadNode> nodes = new ArrayList<KadNode>();
	private int maxNodeCount = 1;
	private byte[] content;
	private int connPort = 0;
	private boolean keepAlive = false;
	
	public KadMessageBuilder(KadMessage msg) {
		loadKadMessage(msg);
	}
	
	public KadMessageBuilder() {
	}

	public KadMessageBuilder loadKadMessage(KadMessage msg) {
		this.rpc = msg.getRpc();
		this.tag = msg.getTag();
		this.key = msg.getKey();
		this.contacts.addAll(msg.getContacts());
		this.dst = msg.getDst();
		this.path.addAll(msg.getPath());
		this.nodes.addAll(msg.getNodes());
		this.maxNodeCount = msg.getMaxNodeCount();
		this.content = msg.getConent();
		this.connPort = msg.getConnPort();
		this.keepAlive = msg.isKeepAlive();
		
		return this;
	}

	public KadMessage build() {
		if (rpc == null)
			throw new IllegalArgumentException("msg missing rpc");
		if (path.isEmpty())
			throw new IllegalArgumentException("msg missing src");
		//nodes = Collections.unmodifiableList(nodes);
		
		return new KadMessage(rpc, tag, key, contacts, path, dst, nodes, maxNodeCount, content, connPort, keepAlive);
	}
	
	public KadMessageBuilder setDst(Key dst) {
		this.dst = dst;
		return this;
	}
	
	public KadMessageBuilder setContent(byte[] content) {
		this.content = content;
		return this;
	}
	
	public KadMessageBuilder addHop(KadNode n) {
		this.path.add(n);
		return this;
	}
	
	public KadMessageBuilder setRpc(RPC rpc) {
		this.rpc = rpc;
		return this;
	}
	
	public KadMessageBuilder setMaxNodeCount(int maxNodeCount) {
		this.maxNodeCount = maxNodeCount;
		return this;
	}
	
	public KadMessageBuilder setTag(String tag) {
		this.tag = tag;
		return this;
	}

	public KadMessageBuilder setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public KadMessageBuilder addNodes(Collection<KadNode> nodes) {
		this.nodes.addAll(nodes);
		return this;
	}
	
	public void sendTo(KadConnection conn) throws IOException {
		conn.sendMessage(this.build());
	}

	public KadMessageBuilder fixLastHopAddress(InetAddress addr) {
		int i = path.size()-1;
		KadNode n = path.get(i).setAddr(addr);
		path.set(i, n);
		return this;
	}
	
	public KadMessageBuilder setConnPort(int connPort) {
		this.connPort = connPort;
		return this;
	}
	
	public KadMessageBuilder setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
		return this;
	}
	

	public KadMessageBuilder addContacts(Collection<Key> contacts) {
		this.contacts.addAll(contacts);
		return this;
	}
	
}
