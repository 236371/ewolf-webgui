package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.openkad.KadConnectionListener;
import il.technion.ewolf.kbr.openkad.KadEndpoint;
import il.technion.ewolf.kbr.openkad.KadKeyFactory;
import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;
import il.technion.ewolf.kbr.openkad.KadNode;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.junit.Test;

public class KadConnectionTest implements KadConnectionListener {

	private final KadNode localNode;
	private final KeyFactory keyFactory;
	
	public KadConnectionTest() throws Exception {
		keyFactory = new KadKeyFactory(20, 0, "SHA-256");
		List<KadEndpoint> endpoints = new ArrayList<KadEndpoint>();
		localNode = new KadNode(keyFactory.generate(),
				InetAddress.getLocalHost(),
				endpoints);
	}
	
	
	@Test(timeout=5000)
	public void test1() throws Exception {
		KadServer endpoint  = new KadServer(1024*16, Executors.newFixedThreadPool(2));
		endpoint.setKadConnectionListener(this);
		endpoint.register(KadProtocol.otcpkad, new InetSocketAddress(10000));
		endpoint.register(KadProtocol.oudpkad, new InetSocketAddress(10000));
		
		new Thread(endpoint).start();
		Thread.sleep(1000);
		KadConnection conn1 = KadProtocol.otcpkad.openConnection(new InetSocketAddress(InetAddress.getLocalHost(), 10000));
		KadConnection conn2 = KadProtocol.oudpkad.openConnection(new InetSocketAddress(InetAddress.getLocalHost(), 10000));
		KadMessage recved;
		
		
		byte[] b = new byte[4096];
		
		new KadMessageBuilder()
			.setRpc(RPC.PING)
			.addHop(localNode)
			.setContent(b)
			.sendTo(conn1);
		recved = conn1.recvMessage();
		conn1.close();
		Assert.assertEquals("abc", recved.getTag());
		
		
		new KadMessageBuilder()
			.setRpc(RPC.PING)
			.addHop(localNode)
			.setContent(b)
			.sendTo(conn2);
		
		recved = conn2.recvMessage();
		conn2.close();
		Assert.assertEquals("abc", recved.getTag());
		
	}

	@Override
	public void onIncomingConnection(KadConnection conn) throws IOException {
		KadMessageBuilder response = new KadMessageBuilder();
		onIncomingMessage(conn.recvMessage(), response);
		conn.sendMessage(response.build());
		conn.close();
	}

	@Override
	public void onIncomingMessage(KadMessage msg, KadMessageBuilder response) throws IOException {
		response
			.addHop(localNode)
			.setTag("abc")
			.setRpc(RPC.PING);
	}
}
