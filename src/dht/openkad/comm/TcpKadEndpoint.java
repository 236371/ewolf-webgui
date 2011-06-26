package dht.openkad.comm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import dht.openkad.KadConnectionListener;
import dht.openkad.KadEndpoint;

public class TcpKadEndpoint extends KadEndpoint implements Runnable {

	
	private InetSocketAddress addr;
	private ServerSocket srvSock;
	private KadConnectionListener listener;
	
	private boolean active = true;
	
	TcpKadEndpoint(InetSocketAddress addr) {
		this.addr = addr;
	}
	
	
	@Override
	public void publish(KadConnectionListener listener) throws IOException {
		srvSock = new ServerSocket(addr.getPort());
		this.listener = listener;
		new Thread(this).start();
	}


	@Override
	public void run() {
		while (active) {
			try {
				listener.onIncomingConnection(new TcpKadConnection(srvSock.accept()));
			} catch (Exception e) {}
		}
	}
	@Override
	public String toString() {
		return "tcpkad://"+addr.getHostName()+":"+addr.getPort()+"/";
	}


	@Override
	public void shutdown() {
		active = false;
		try {
			srvSock.close();
		} catch (IOException e) {}
	}
	
}
