package dht.openkad.comm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import dht.openkad.KadConnection;
import dht.openkad.KadMsg;
import dht.openkad.KadMsgBuilder;
import dht.openkad.KadNode;
import dht.openkad.validator.KadMsgValidator;

public class TcpKadConnection extends KadConnection {

	private Socket sock;
	
	TcpKadConnection(InetSocketAddress addr) throws IOException {
		this(new Socket(addr.getAddress(), addr.getPort()));
	}
	
	TcpKadConnection(Socket sock) {
		this.sock = sock;
	}
	
	@Override
	public void sendMessage(KadMsg msg) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
		oos.writeObject(msg);
		oos.flush();
	}

	@Override
	public KadMsg recvMessage(KadMsgValidator ... validators) throws IOException {
		ObjectInputStream oin = new ObjectInputStream(sock.getInputStream());
		try {
			KadMsg msg = (KadMsg)oin.readObject();
			
			// fix the source ip address
			KadNode src = msg.getSrc().setAddr(sock.getInetAddress());
			
			KadMsg $ = new KadMsgBuilder(msg)
				.setSrc(src)
				.buildMessage();
			
			for (int i=0; i < validators.length; ++i) {
				validators[i].validate($);
			}
			
			return $;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void close() {
		try { sock.close(); } catch (IOException e) {}
	}

}
