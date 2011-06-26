package dht.openkad;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import dht.DHT;
import dht.Key;
import dht.KeyFactory;
import dht.SerializerFactory;
import dht.openkad.KadMsg.RPC;
import dht.openkad.ops.KadOperationsExecutor;
import dht.openkad.validator.KadBasicMsgValidator;
import dht.openkad.validator.KadMsgValidator;

class Kademlia implements DHT, KadConnectionListener {

	private final KeyFactory keyFactory;
	private final KBucketsList kbuckets;
	private final List<KadEndpoint> endpoints;
	private final KadNode localNode;
	private final LocalStorage localStorage;
	private final SerializerFactory defaultSerializer;
	private final KadOperationsExecutor opExecutor;
	private final KadConnectionDispacher connDispacher;
	private final KadMsgValidator basicValidator;
	
	@Inject
	Kademlia(KeyFactory keyFactory,
			 KBucketsList kbuckets,
			 @Named("kad.endpoints") List<KadEndpoint> endpoints,
			 @Named("kad.localnode") KadNode localNode,
			 LocalStorage localStorage,
			 SerializerFactory defaultSerializer,
			 KadOperationsExecutor opExecutor,
			 KadConnectionDispacher connDispacher) {
		
		this.keyFactory = keyFactory;
		this.kbuckets = kbuckets;
		this.endpoints = endpoints;
		this.localNode = localNode;
		this.localStorage = localStorage;
		this.defaultSerializer = defaultSerializer;
		this.opExecutor = opExecutor;
		this.connDispacher = connDispacher;
		this.basicValidator = new KadBasicMsgValidator(keyFactory);
		
	}
	

	@Override
	public void create() {
		connDispacher.setConnListener(this);
		for (KadEndpoint endpoint : endpoints) {
			try {
				endpoint.publish(connDispacher);
			} catch (IOException e) {
				throw new RuntimeException("Could not publish endpoint: "+endpoint, e);
			}
		}
		System.out.println(localNode);
	}

	@Override
	public void join(URI bootstrap) throws UnknownHostException {
		KadNode bootstrapNode = new KadNode(keyFactory, bootstrap);
		try {
			opExecutor.createJoinOperation(bootstrapNode).call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	@Override
	public Future<Void> put(Key key, Serializable value) throws IOException {
		return put(key, value, defaultSerializer);
	}
	
	@Override
	public Future<Void> put(Key key, Object value, SerializerFactory serializer) throws IOException {
		return opExecutor.submitStoreValue(key, value, serializer);
	}

	
	@Override
	public Set<Object> get(Key key) throws IOException {
		return get(key, defaultSerializer);
	}
	
	@Override
	public Set<Object> get(Key key, SerializerFactory serializer) throws IOException {
		try {
			return opExecutor.createFindValues(key, serializer).call();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError("should never happen");
		}
	}

	
	@Override
	public KeyFactory getKeyFactory() {
		return keyFactory;
	}

	
	@Override
	public void onIncomingConnection(KadConnection conn) throws IOException {
		try {
			KadMsg msg = conn.recvMessage(basicValidator);
			opExecutor.executeInsertNode(msg.getSrc());
			switch(msg.getRpc()) {
			case PING:
				new KadMsgBuilder()
					.setSrc(localNode)
					.setRpc(RPC.PING)
					.sendTo(conn);
				break;
			case STORE:
				localStorage.putAll(msg.getKey(), msg.getValues());
				break;
			case FIND_NODE:
				new KadMsgBuilder()
					.setSrc(localNode)
					.setRpc(RPC.FIND_NODE)
					.setKey(msg.getKey())
					.addCloseNodes(kbuckets.getKClosestNodes(msg.getKey()))
					.sendTo(conn);
				break;
			case FIND_VALUE:
				new KadMsgBuilder()
					.setSrc(localNode)
					.setRpc(RPC.FIND_VALUE)
					.setKey(msg.getKey())
					.addCloseNodes(kbuckets.getKClosestNodes(msg.getKey()))
					.addValues(localStorage.get(msg.getKey()))
					.sendTo(conn);
				break;
			}
		} finally {
			conn.close();
		}
	}

	@Override
	public Key getNodeID() {
		return localNode.getKey();
		
	}
	
	@Override
	public String toString() {
		return getNodeID() +"\n***********************\n"+kbuckets.toString();
	}

	@Override
	public Set<Object> localGet(Key key) throws IOException {
		return localGet(key, defaultSerializer);
	}
	
	@Override
	public Set<Object> localGet(Key key, SerializerFactory serializer) throws IOException {
		Set<Object> $ = new HashSet<Object>();
		for (byte[] b : localStorage.get(key)) {
			try {
				$.add(serializer.createObjectInput(new ByteArrayInputStream(b)).readObject());
			} catch (ClassNotFoundException e) {
				System.err.println("warning: incompatable type");
			}
		}
		return $;
	}



	@Override
	public void shutdown() {
		for (KadEndpoint e : endpoints)
			e.shutdown();
	}
	
	
}
