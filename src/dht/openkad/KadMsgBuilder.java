package dht.openkad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dht.Key;
import dht.SerializerFactory;
import dht.openkad.KadMsg.RPC;

public class KadMsgBuilder {

	private RPC rpc = null;
	private KadNode src = null;
	private Key key = null;
	private List<KadNode> knownClosestNodes = new ArrayList<KadNode>();
	private List<byte[]> values = new ArrayList<byte[]>();
	
	public KadMsgBuilder() {
	}

	public KadMsgBuilder(KadMsg msg) {
		rpc = msg.getRpc();
		src = msg.getSrc();
		key = msg.getKey();
		knownClosestNodes = msg.getKnownClosestNodes();
		values = msg.getValues();
	}
	
	public KadMsgBuilder setRpc(RPC rpc) {
		this.rpc = rpc;
		return this;
	}

	public KadMsgBuilder setSrc(KadNode src) {
		this.src = src;
		return this;
	}

	public KadMsgBuilder setKey(Key key) {
		this.key = key;
		return this;
	}

	public KadMsgBuilder addValues(byte[] ... values) {
		addValues(Arrays.asList(values));
		return this;
	}
	public KadMsgBuilder addValues(Collection<byte[]> values) {
		for (byte[] b : values)
			this.values.add(b);
		return this;
	}
	
	public KadMsgBuilder addValues(SerializerFactory serializer, Object ... values) throws IOException {
		return addValues(serializer, Arrays.asList(values));
	}
	
	public KadMsgBuilder addValues(SerializerFactory serializer, Collection<Object> values) throws IOException {
		for (Object s : values) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutput objectOut = serializer.createObjectOutput(out);
			objectOut.writeObject(s);
			objectOut.close();
			this.values.add(out.toByteArray());
		}
		return this;
	}
	
	public KadMsgBuilder addCloseNodes(List<KadNode> n) {
		knownClosestNodes.addAll(n);
		return this;
	}

	
	public KadMsg buildMessage() {
		if (rpc == null || ((src == null || key == null) && rpc != RPC.PING))
			throw new IllegalArgumentException("rpc, src and key cannot b null");
		
		values = Collections.unmodifiableList(values);
		knownClosestNodes = Collections.unmodifiableList(knownClosestNodes);
		
		return new KadMsg(rpc, src, key,
				knownClosestNodes.toArray(new KadNode[0]),
				values);
	}
	
	public KadConnection sendTo(KadNode n) throws IOException {
		KadMsg msg = buildMessage();
		KadConnection conn = n.openConnection();
		conn.sendMessage(msg);
		return conn;
	}
	
	
	public void sendTo(KadConnection conn) throws IOException {
		conn.sendMessage(buildMessage());
	}

}
