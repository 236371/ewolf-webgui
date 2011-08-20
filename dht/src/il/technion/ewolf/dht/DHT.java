package il.technion.ewolf.dht;

import static ch.lambdaj.Lambda.convert;
import static ch.lambdaj.Lambda.filter;
import static org.hamcrest.Matchers.nullValue;
import il.technion.ewolf.dht.DHTMessage.RPC;
import il.technion.ewolf.kbr.DefaultNodeConnectionListener;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.Node;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import encoding.Base64;

public class DHT {
	
	
	private final KeybasedRouting kbr;
	private final String name;
	private final int replication;
	private final int replicationSaftyMargin;
	private final ExecutorService executor;
	private final Random rnd;
	private final long getOperationTimeout;
	private final DHTStorage storage;
	
	@Inject
	DHT(@Named("dht.random") Random rnd,
		@Named("dht.name") String name,
		@Named("dht.get.timeout") long getOperationTimeout,
		@Named("dht.replication.factor") int replication,
		@Named("dht.replication.saftymargin") int replicationSaftyMargin,
		@Named("dht.executor") ExecutorService executor,
		DHTStorage storage,
		KeybasedRouting kbr) {
		
		
		this.rnd = rnd;
		this.getOperationTimeout = getOperationTimeout;
		this.kbr = kbr;
		this.name = name;
		this.replication = replication;
		this.replicationSaftyMargin = replicationSaftyMargin;
		this.executor = executor;
		this.storage = storage;
		
	}
	
	private int sendMessageToAll(String tag, byte[] serializedMessage, Node ... nodes) {
		return sendMessageToAll(tag, serializedMessage, Arrays.asList(nodes));
	}
	
	private int sendMessageToAll(String tag, byte[] serializedMessage, List<Node> nodes) {
		int sent = 0;
		for (int i=0; i < nodes.size() && sent < replication; ++i) {
			OutputStream out = null;
			try {
				out = nodes.get(i).sendMessage(tag);
				out.write(serializedMessage);
			} catch (Exception e) {
				out = null;
			} finally {
				try { out.close(); ++sent; } catch (Exception e) {}
			}
		}
		return sent;
	}
	
	
	public String getName() {
		return name;
	}
	
	public String getTag() {
		return "dht_"+getName();
	}
	
	public void start() {
		kbr.register(getTag(), new DefaultNodeConnectionListener() {
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in) throws IOException {
				
				assert (tag.equals(getName()));
				
				DHTMessage msg = new DHTMessageBuilder(in).build();
				
				switch (msg.getRpc()) {
				case STORE:
					storage.store(msg.getKey(), msg.getData());
					break;
					
				case FIND_VALUE:
					
					byte[] serializedResponse = new DHTMessageBuilder()
						.setKey(msg.getKey())
						.setRpc(RPC.FIND_VALUE_RESPONSE)
						.addAllData(storage.get(msg.getKey()))
						.build()
						.toJson()
						.getBytes();
					sendMessageToAll(msg.getResponseTag(), serializedResponse, from);
					break;
				}
			}
		});
	}
	
	public Future<Integer> put(String keyString, byte[] data) {
		Key key = kbr.getKeyFactory().getFromData(keyString);
		final Future<List<Node>> nodesFuture = kbr.findNodes(key, replicationSaftyMargin);
		
		final byte[] storeMessage = new DHTMessageBuilder()
				.setRpc(RPC.STORE)
				.setKey(key)
				.addData(Base64.encodeBytes(data))
				.build()
				.toJson()
				.getBytes();
		
		
		return executor.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return sendMessageToAll(getTag(), storeMessage, nodesFuture.get());
			}
		});
		
	}


	public Future<List<byte[]>> get(String keyString) {
		Key key = kbr.getKeyFactory().getFromData(keyString);
		final Future<List<Node>> nodesFuture = kbr.findNodes(key, replicationSaftyMargin);
		
		final String responseTag = getTag() + "_" + Integer.toHexString(rnd.nextInt());
		
		final byte[] findValueMessage = new DHTMessageBuilder()
				.setRpc(RPC.FIND_VALUE)
				.setKey(key)
				.setResponseTag(responseTag)
				.build()
				.toJson()
				.getBytes();
		
		return executor.submit(new Callable<List<byte[]>>() {
			@Override
			public List<byte[]> call() throws Exception {
				
				final Set<String> results = new HashSet<String>();
				
				kbr.register(responseTag, new DefaultNodeConnectionListener() {
					@Override
					public void onIncomingMessage(String tag, Node from, InputStream in) throws IOException {
						DHTMessage msg = new DHTMessageBuilder(in).build();
						
						synchronized(results) {
							results.addAll(msg.getData());
							results.notifyAll();
						}
					}
				});
				
				try {
					int n = sendMessageToAll(getTag(), findValueMessage, nodesFuture.get());
					
					// wait while recving messages
					long timeout = System.currentTimeMillis() + getOperationTimeout;
					synchronized (results) {
						while (results.size() < n && System.currentTimeMillis() < timeout) {
							results.wait(getOperationTimeout);
						}
					}
				} finally {
					kbr.unregister(responseTag);
				}
				
				if (results.isEmpty())
					return new ArrayList<byte[]>();
					
				return filter(nullValue(), convert(results, new Base64Converter()));
			}
		});
	}
}
