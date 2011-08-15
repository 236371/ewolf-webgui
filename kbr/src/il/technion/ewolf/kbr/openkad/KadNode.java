package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyHolder;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.net.KadConnection;
import il.technion.ewolf.kbr.openkad.ops.KadOperationsExecutor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;


public class KadNode implements Serializable, KeyHolder {
	
	private static final long serialVersionUID = -1726068490394037080L;
	
	private final InetAddress addr;
	private final List<KadEndpoint> endpoints;
	private final Key key;
	
	private final List<KadNode> proxies;
	
	public KadNode(Key key, InetAddress addr, List<KadEndpoint> endpoints) {
		this(key, addr, endpoints, new ArrayList<KadNode>());
	}
	
	
	public KadNode(Key key, InetAddress addr, List<KadEndpoint> endpoints, List<KadNode> proxies) {
		this.key = key;
		this.addr = addr;
		this.endpoints = Collections.unmodifiableList(endpoints);
		this.proxies = Collections.synchronizedList(proxies);
	}
	
	public void addProxy(KadNode n) {
		if (this.equals(n))
			return;
		proxies.add(n);
	}
	
	public void mergeProxies(KadNode n) {
		List<KadNode> nProxies = new ArrayList<KadNode>();
		synchronized (n.proxies) {
			nProxies.addAll(n.proxies);
		}
		for (KadNode x : nProxies) {
			if (!proxies.contains(x))
				proxies.add(x);
		}
	}
	
	public InetAddress getAddress() {
		return addr;
	}
	
	public KadNode setAddr(InetAddress addr) {
		return new KadNode(getKey(), addr, endpoints);
	}
	
	List<KadEndpoint> getEndpoints() {
		return endpoints;
	}
	
	@Override
	public Key getKey() {
		return key;
	}
	
	List<KadConnection> getDirectKadConnections() {
		List<KadConnection> $ = new ArrayList<KadConnection>();
		for (KadEndpoint endpoint : endpoints) {
			try {
				$.add(endpoint.openConnection(addr));
			} catch (Exception e) {}
		}
		return $;
	}
	
	public List<KadConnection> getKadConnections() {
		List<KadConnection> $ = getDirectKadConnections();
		
		for (KadNode n : proxies) {
			$.addAll(n.getDirectKadConnections());
		}
		
		return $;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o.getClass().equals(this.getClass())))
			return false;
		return getKey().equals(((KadNode)o).getKey());
	}
	
	@Override
	public int hashCode() {
		return getKey().hashCode();
	}
	
	@Override
	public String toString() {
		return getKey().toString();
	}
	
	Node getNode(final KadOperationsExecutor opExecutor) {
		return new Node() {

			@Override
			public boolean equals(Object obj) {
				if (obj == null || !getClass().equals(obj.getClass()))
					return false;
				return getKey().equals(((Node)obj).getKey());
			}
			
			@Override
			public int hashCode() {
				return getKey().hashCode();
			}
			
			@Override
			public Key getKey() {
				return KadNode.this.getKey();
			}

			@Override
			public Future<Socket> openConnection(String tag) throws IOException {
				return opExecutor.submitOpenConnectionOperation(KadNode.this, tag);
			}

			@Override
			public OutputStream sendMessage(String tag) throws IOException {
				return new MessageOutputStream(opExecutor.getLocalNode(), KadNode.this, tag);
			}
			
			@Override
			public String toString() {
				return getKey().toString();
			}
			
			@Override
			public Future<DatagramSocket> openUdpConnection(String tag)
					throws IOException {
				// TODO: implement
				return null;
			}
			
			@Override
			public byte[] sendMessage(String tag, byte[] message)
					throws IOException {
				// TODO implement
				return null;
			}
			
		};
	}
}
