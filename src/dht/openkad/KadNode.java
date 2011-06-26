package dht.openkad;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dht.Key;
import dht.KeyFactory;
import dht.openkad.comm.KadProtocol;

public class KadNode implements Serializable {

	private static final long serialVersionUID = 8267380586683676866L;
	 
	private final Key key;
	private final InetAddress addr;
	private final Contact[] contact;
	
	static final class Contact implements Serializable {
		
		private static final long serialVersionUID = -7838552547557967106L;
		
		private final String protocol;
		private final int port;
		
		public Contact(String protocol, int port) {
			this.protocol = protocol;
			this.port = port;
		}
		public String getProtocol() {
			return protocol;
		}
		public int getPort() {
			return port;
		}
		
		public String toString() {
			return "("+protocol+":"+port+")";
		}
		
	}
	private KadNode(Key key, InetAddress addr, Contact ... contact) {
		this.key = key;
		this.addr = addr;
		this.contact = contact;
	}
	
	public KadNode setAddr(InetAddress addr) {
		return new KadNode(key, addr, contact);
	}
	
	List<Contact> getContacts() {
		 return Arrays.asList(contact);
	}
	
	InetAddress getAddr() {
		return addr;
	}
	
	public KadNode(Key key, KeyFactory kf, InetAddress addr, Contact ... contact) {
		if (key.getBytes().length != kf.getByteCount()) {
			throw new IllegalArgumentException("the key provided ("+key+") is not a valid kademlia key");
		}
		this.key = key;
		this.addr = addr;
		this.contact = contact;
	}
	
	KadNode(KeyFactory kf, URI uri) throws UnknownHostException {
		key = kf.getFromKey(uri.getPath().substring(1));
		addr = InetAddress.getByName(uri.getHost());
		
		this.contact = new Contact[] { new Contact(uri.getScheme(), uri.getPort()) };
	}
	
	KadNode(KeyFactory kf, Contact ... contacts) {
		this(kf, (InetAddress)null, contacts);
	}
	
	KadNode(KeyFactory kf, InetAddress addr, Contact ... contacts) {
		key = kf.generate();
		try {
			this.addr = addr == null ? InetAddress.getLocalHost() : addr;
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
		this.contact = contacts;
	}
	
	/*
	public void addContact(String protocol, int port) {
		contact.add(new Contact(protocol, port));
	}
	*/
	
	public KadConnection openConnection() throws IOException {
		for (Contact c : contact) {
			try {
				return KadProtocol.valueOf(c.getProtocol())
					.openConnection(new InetSocketAddress(addr, c.getPort()));
			} catch (IllegalArgumentException e) {
				System.err.println("Unsupported protocol: "+c.getProtocol());
			} catch (IOException e) {
				System.err.println("Unable to connect using protocol: "+c.getProtocol());
			}
		}
		throw new IOException("Could not open connection with node "+key);
	}

	public Key getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		KadNode other = (KadNode) obj;
		return getKey().equals(other.getKey());
	}
	
	@Override
	public String toString() {
		
		List<String> $ = new ArrayList<String>();
		for (Contact c : contact) {
			$.add(c.getProtocol()+"://"+addr.getHostAddress()+":"+c.port+"/");
		}
		return key.toString() + ": "+$.toString();
	}
	
	
	public boolean validate(KeyFactory keyFactory) {
		if (key == null || addr == null || contact == null)
			return false;
		if (key.getBytes().length != keyFactory.getByteCount() || contact.length == 0)
			return false;
		
		return true;
	}
	
}
