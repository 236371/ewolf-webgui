package il.technion.ewolf.kbr.openkad.op;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.PingRequest;
import il.technion.ewolf.kbr.openkad.msg.PingResponse;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class JoinOperation implements Callable<Void> {

	//dependencies
	private final Provider<FindNodeOperation> findNodeOperationProvider;
	private final Provider<PingRequest> pingRequestProvider;
	private final Provider<MessageDispatcher<Void>> msgDispatcherProvider;
	private final Key zeroKey;
	private final String kadScheme;
	private final KBuckets kBuckets;
	private final Node localNode;
	private final Provider<KadNode> kadNodeProvider;
	
	// state
	private Collection<Node> bootstrap = new HashSet<Node>();
	
	@Inject
	JoinOperation(
			Provider<FindNodeOperation> findNodeOperationProvider,
			Provider<PingRequest> pingRequestProvider,
			Provider<MessageDispatcher<Void>> msgDispatcherProvider,
			Provider<KadNode> kadNodeProvider,
			KBuckets kBuckets,
			@Named("openkad.keys.zerokey") Key zeroKey,
			@Named("openkad.scheme.name") String kadScheme,
			@Named("openkad.local.node") Node localNode) {
		
		this.kadNodeProvider = kadNodeProvider;
		this.findNodeOperationProvider = findNodeOperationProvider;
		this.pingRequestProvider = pingRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.kBuckets = kBuckets;
		this.zeroKey = zeroKey;
		this.kadScheme = kadScheme;
		this.localNode = localNode;
	}
	
	public JoinOperation addBootstrap(Collection<URI> bootstrapUri) {
		
		for (URI uri : bootstrapUri) {
			Node n = new Node(zeroKey);
			try {
				n.setInetAddress(InetAddress.getByName(uri.getHost()));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				continue;
			}
			n.addEndpoint(kadScheme, uri.getPort());
			bootstrap.add(n);
		}
		
		return this;
	}
	
	@Override
	public Void call() throws Exception {

		final CountDownLatch latch = new CountDownLatch(bootstrap.size());
		CompletionHandler<KadMessage, Void> callback = new CompletionHandler<KadMessage, Void>() {

			@Override
			public void completed(KadMessage msg, Void nothing) {
				try {
					kBuckets.insert(kadNodeProvider.get()
						.setNode(msg.getSrc())
						.setNodeWasContacted());
				} finally {
					latch.countDown();
				}
			}

			@Override
			public void failed(Throwable exc, Void nothing) {
				latch.countDown();
			}
		};
		
		for (Node n : bootstrap) {
			PingRequest pingRequest = pingRequestProvider.get();
			msgDispatcherProvider.get()
				.addFilter(new IdMessageFilter(pingRequest.getId()))
				.addFilter(new TypeMessageFilter(PingResponse.class))
				.setConsumable(true)
				.setCallback(null, callback)
				.send(n, pingRequest);
		}
		
		// waiting for responses
		
		latch.await();
		
		
		
		if (kBuckets.getClosestNodesFromKBuckets(zeroKey, 1).isEmpty())
			throw new IllegalStateException("all bootstrap nodes are down");
		
		
		
		findNodeOperationProvider.get()
			.setKey(localNode.getKey())
			.call();
		
		
		
		for (Key key : kBuckets.randomKeysForAllBuckets()) {
			findNodeOperationProvider.get()
				.setKey(key)
				.call();
		}
		
		
		
		//System.out.println(localNode+": \n"+kBuckets);
		
		return null;
	}

}
