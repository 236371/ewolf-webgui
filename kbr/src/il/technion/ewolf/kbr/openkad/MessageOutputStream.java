package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


class MessageOutputStream extends ByteArrayOutputStream {
	
	private final KadNode localNode;
	private final KadNode dstNode;
	private final String tag;
	
	MessageOutputStream(
			KadNode localNode,
			KadNode dstNode,
			String tag) {
		this.localNode = localNode;
		this.dstNode = dstNode;
		this.tag = tag;
	}
	
	@Override
	public void close() throws IOException {
		flush();
		super.close();
	}
	
	@Override
	public synchronized void flush() throws IOException {
		if (count == 0)
			return;
		
		KadMessage msg = new KadMessageBuilder()
			.addHop(localNode)
			.setRpc(RPC.MSG)
			.setDst(dstNode.getKey())
			.setContent(Arrays.copyOf(buf, count))
			.setTag(tag)
			.build();
		
		List<KadConnection> kadConnections = dstNode.getKadConnections();
		
		while (!kadConnections.isEmpty()) {
			KadConnection conn = kadConnections.remove(0);
			try {
				conn.sendMessage(msg);
				// recv ack
				conn.recvMessage();
				
				// close all connections
				for (KadConnection c : kadConnections)
					c.close();
				reset();
				return;
			} catch (IOException e) {
				// error sending or recving in conn
			} finally {
				conn.close();
			}
		}
		throw new IOException("error sending/recving");
	}
}
