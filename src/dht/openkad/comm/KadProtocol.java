package dht.openkad.comm;

import java.io.IOException;
import java.net.InetSocketAddress;

import dht.openkad.KadConnection;
import dht.openkad.KadEndpoint;

public enum KadProtocol {

	tcpkad {

		@Override
		public KadConnection openConnection(InetSocketAddress addr) throws IOException {
			return new TcpKadConnection(addr);
		}
		
		@Override
		public KadEndpoint createEndpoing(InetSocketAddress addr) {
			return new TcpKadEndpoint(addr);
		}
		
	};
	
	
	
	public abstract KadConnection openConnection(InetSocketAddress addr) throws IOException;
	
	public abstract KadEndpoint createEndpoing(InetSocketAddress addr);
}
