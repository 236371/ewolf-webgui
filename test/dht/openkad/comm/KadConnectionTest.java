package dht.openkad.comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import dht.KeyFactory;
import dht.SerializerFactory;
import dht.openkad.KadConnection;
import dht.openkad.KadConnectionListener;
import dht.openkad.KadEndpoint;
import dht.openkad.KadKeyFactory;
import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;
import dht.openkad.KadMsgBuilder;
import dht.openkad.KadNode;

public class KadConnectionTest {

	private SerializerFactory serializer = new SerializerFactory() {

		@Override
		public ObjectInput createObjectInput(InputStream is) throws IOException {
			return new ObjectInputStream(is);
		}

		@Override
		public ObjectOutput createObjectOutput(OutputStream os)
				throws IOException {
			return new ObjectOutputStream(os);
		}
	};
	
	@Test(timeout=5000)
	public void tcpkad() throws Exception {
		KeyFactory kf = new KadKeyFactory();
		KadNode localNode = new KadNode(kf.generate(), kf, InetAddress.getLocalHost());
		InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 5678);
		final List<String> someData = new ArrayList<String>();
		someData.add("val1");
		someData.add("val2");
		someData.add("val3");
		
		KadEndpoint endpoint = KadProtocol.tcpkad.createEndpoing(addr);
		endpoint.publish(new KadConnectionListener() {
			
			@Override
			public void onIncomingConnection(KadConnection conn) throws IOException {
				KadMsg msg = conn.recvMessage();
				Assert.assertEquals(RPC.PING, msg.getRpc());
				try {
					Object o = serializer.createObjectInput(new ByteArrayInputStream(msg.getValues().get(0))).readObject();
					Assert.assertEquals(someData, o);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				conn.sendMessage(msg);
			}
		});
		// wait until the endpoint is published
		Thread.sleep(500);
		KadConnection conn = KadProtocol.tcpkad.openConnection(addr);
		new KadMsgBuilder()
			.setRpc(RPC.PING)
			.setSrc(localNode)
			.addValues(serializer, (Object)someData)
			.sendTo(conn);
		KadMsg recv = conn.recvMessage();
		Assert.assertEquals(RPC.PING, recv.getRpc());
		conn.close();
		
	}
}
