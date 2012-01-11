package il.technion.ewolf.kbr;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node implements Serializable {

	private static final long serialVersionUID = 2520444508318328765L;
	
	private final Key key;
	//private final Certificate certificate;
	private InetAddress addr = null;
	private Map<String, Integer> portFromScheme = new HashMap<String, Integer>();
	
	// dummy node
	public Node() {
		this(null);
	}
	
	public Node(/*Certificate certificate, */Key key) {
		//this.certificate = certificate;
		this.key = key;
	}
	
	public Key getKey() {
		return key;
	}
	
	//public Certificate getCertificate() {
	//	return certificate;
	//}
	
	public Map<String, Integer> getAllEndpoints() {
		return portFromScheme;
	}
	
	public URI getURI(String scheme) {
		try {
			return new URI(scheme+"://"+addr.getHostAddress()+":"+getPort(scheme)+"/"+key.toBase64());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<URI> toURIs() {
		List<String> schemes = new ArrayList<String>(portFromScheme.keySet());
		Collections.sort(schemes);
		List<URI> $ = new ArrayList<URI>();
		for (String scheme : schemes)
			$.add(getURI(scheme));
		return $;
	}
	
	public void setEndpoints(Map<String, Integer> portFromScheme) {
		this.portFromScheme = portFromScheme;
	}
	
	public SocketAddress getSocketAddress(String scheme) {
		return new InetSocketAddress(addr, getPort(scheme));
	}
	
	public void addEndpoint(String scheme, int port) {
		portFromScheme.put(scheme, port);
	}
	
	public InetAddress getInetAddress() {
		return addr;
	}
	
	public void setInetAddress(InetAddress addr) {
		this.addr = addr;
	}
	
	public int getPort(String scheme) {
		return portFromScheme.get(scheme);
	}
	
	@Override
	public String toString() {
		return getKey().toString();
		//return portFromScheme.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		return getKey().equals(other.getKey());
	}
	
	
	
}
