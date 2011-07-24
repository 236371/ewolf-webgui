package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.openkad.net.KadConnection;
import il.technion.ewolf.kbr.openkad.net.KadProtocol;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;


public class KadEndpoint implements Serializable {

	private static final long serialVersionUID = 1188893303445111858L;
	
	private final String protocol;
	private final int port;
	
	public KadEndpoint(String protocol, int port) {
		this.protocol = protocol;
		this.port = port;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public KadProtocol getKadProtocol() {
		return KadProtocol.valueOf(protocol);
	}
	
	public int getPort() {
		return port;
	}
	

	public KadConnection openConnection(InetAddress addr) throws IOException {
		return getKadProtocol().openConnection(new InetSocketAddress(addr, getPort()));
	}
	
}
