package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;


public class OpenConnectionOperation extends KadOperation<Socket> {

	
	private final KadNode localNode;
	private final KadNode dstNode;
	private final String tag;
	private final int connPort;
	
	OpenConnectionOperation(Logger logger, KadNode localNode, int connPort, KadNode dstNode, String tag) {
		super(logger);
		this.localNode = localNode;
		this.connPort = connPort;
		this.dstNode = dstNode;
		this.tag = tag;
	}
	
	
	private ServerSocket initServerSocket() {
		try {
			return new ServerSocket(connPort);
		} catch (Exception e) {
		}
		return null;
	}
	
	private Socket accept(ServerSocket srvSock) {
		try {
			srvSock.setSoTimeout(1000);
			return srvSock.accept();
		} catch (Exception e) {
		}
		return null;
	}
	
	private Socket connect(KadMessage ackMsg) {
		try {
			return new Socket(ackMsg.getLastHop().getAddress(), ackMsg.getConnPort());
		} catch (Exception e) {
		}
		return null;
	}
	
	@Override
	public Socket call() throws Exception {
		ServerSocket srvSock = initServerSocket();
		
		KadMessageBuilder msgBuilder = new KadMessageBuilder()
			.addHop(localNode)
			.setRpc(RPC.CONN)
			.setDst(dstNode.getKey())
			.setTag(tag);
		
		if (srvSock != null)
			msgBuilder.setConnPort(srvSock.getLocalPort());
	
		KadMessage msg = msgBuilder.build();
		List<KadConnection> kadConnections = dstNode.getKadConnections();
		Socket $ = null;
		try {
			while ($ == null && !kadConnections.isEmpty()) {
				KadConnection conn = kadConnections.get(0);
				try {
					conn.sendMessage(msg);
					$ = accept(srvSock);
					if ($ == null) {
						System.err.println("DID NOT ACCEPT !!!");
						// something went wrong with the connection
						// probably im behind a firewall
						
						// try to connect to the remote host
						
						// recv ack
						KadMessage ackMsg = conn.recvMessage();
						$ = connect(ackMsg);
						if ($ == null) {
							// we are both behind a firewall
							throw new UnsupportedOperationException();
						}
					}
				} catch (IOException e) {
					// error sending or recving in conn
					// remove this connection and try the next one
					conn.close();
					kadConnections.remove(conn);
				}
			}
		} finally {
			try { srvSock.close(); } catch (Exception e) {}
		}
		
		// close all connections
		for (KadConnection c : kadConnections)
			c.close();
			
		return $;
	}

}
