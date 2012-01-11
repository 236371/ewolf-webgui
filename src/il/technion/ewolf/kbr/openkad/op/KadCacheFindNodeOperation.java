package il.technion.ewolf.kbr.openkad.op;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.KeyComparator;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.msg.FindNodeRequest;
import il.technion.ewolf.kbr.openkad.msg.FindNodeResponse;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.StoreMessage;
import il.technion.ewolf.kbr.openkad.net.KadServer;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class KadCacheFindNodeOperation extends FindValueOperation implements CompletionHandler<KadMessage, Node> {

	// state
	private List<Node> knownClosestNodes;
	private final Set<Node> alreadyQueried;
	private final Set<Node> querying;
	private int nrQueried;
	private List<Node> lastSentTo;
	private Node returnedCachedResults = null;
	
	// dependencies
	private final Provider<FindNodeRequest> findNodeRequestProvider;
	private final Provider<MessageDispatcher<Node>> msgDispatcherProvider;
	private final KBuckets kBuckets;
	private final Node localNode;
	private final int nrShare;
	private final Provider<StoreMessage> storeMessageProvider;
	private final KadServer kadServer;
	private final KadCache cache;
	
	private final AtomicInteger nrLocalCacheHits;
	private final AtomicInteger nrRemoteCacheHits;
	
	@Inject
	KadCacheFindNodeOperation(
			@Named("openkad.local.node") Node localNode,
			@Named("openkad.bucket.kbuckets.maxsize") int kBucketSize,
			@Named("openkad.bucket.color.share") int nrShare,
			Provider<FindNodeRequest> findNodeRequestProvider,
			Provider<MessageDispatcher<Node>> msgDispatcherProvider,
			KBuckets kBuckets,
			Provider<StoreMessage> storeMessageProvider,
			KadServer kadServer,
			KadCache cache,
			
			@Named("openkad.testing.nrLocalCacheHits") AtomicInteger nrLocalCacheHits,
			@Named("openkad.testing.nrRemoteCacheHits") AtomicInteger nrRemoteCacheHits) {
		
		super(kBucketSize);
		
		this.localNode = localNode;
		this.kBuckets = kBuckets;
		this.nrShare = nrShare;
		this.findNodeRequestProvider = findNodeRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.storeMessageProvider = storeMessageProvider;
		this.kadServer = kadServer;
		this.cache = cache;
		
		alreadyQueried = new HashSet<Node>();
		querying = new HashSet<Node>();
		maxNodes = kBucketSize;
		lastSentTo = new LinkedList<Node>();
		
		this.nrLocalCacheHits = nrLocalCacheHits;
		this.nrRemoteCacheHits = nrRemoteCacheHits;
		
	}
	
	
	public int getNrQueried() {
		return nrQueried;
	}
	
	
	private synchronized Node takeUnqueried() {
		for (int i=0; i < knownClosestNodes.size(); ++i) {
			Node n = knownClosestNodes.get(i);
			if (!querying.contains(n) && !alreadyQueried.contains(n)) {
				querying.add(n);
				return n;
			}
		}
		return null;
	}
	
	private boolean hasMoreToQuery() {
		return !querying.isEmpty() || !alreadyQueried.containsAll(knownClosestNodes);
	}
	
	private void sendFindNode(Node to) {
		FindNodeRequest findNodeRequest = findNodeRequestProvider.get()
			.setSearchCache(true)
			.setMaxNodes(maxNodes)
			.setKey(key);
		
		msgDispatcherProvider.get()
			.addFilter(new IdMessageFilter(findNodeRequest.getId()))
			.addFilter(new TypeMessageFilter(FindNodeResponse.class))
			.setConsumable(true)
			.setCallback(to, this)
			.send(to, findNodeRequest);
	}
	
	private void sendStoreResults(List<Node> toShareWith) {
		toShareWith.remove(returnedCachedResults);
		if (toShareWith.size() > nrShare)
			toShareWith.subList(nrShare, toShareWith.size()).clear();
		
		StoreMessage storeMessage = storeMessageProvider.get()
			.setKey(key)
			.setNodes(knownClosestNodes);
		for (Node n : toShareWith) {
			System.out.println("sharing with: "+n);
			try {
				kadServer.send(n, storeMessage);
			} catch (Exception e) {}
		}
	}
	
	@Override
	public List<Node> call() throws Exception {
		
		List<Node> nodes = cache.search(key);
		if (nodes != null && nodes.size() >= maxNodes) {
			nrLocalCacheHits.incrementAndGet();
			return nodes;
		}
		
		
		knownClosestNodes = kBuckets.getClosestNodesFromKBuckets(key, maxNodes);
		knownClosestNodes.add(localNode);
		alreadyQueried.add(localNode);
		KeyComparator keyComparator = new KeyComparator(key);
		
		do {
			Node n = takeUnqueried();
			
			if (n != null) {
				sendFindNode(n);
			} else {
				synchronized (this) {
					if (!querying.isEmpty())
						wait();
				}
			}
			
			synchronized(this) {
				knownClosestNodes = sort(knownClosestNodes, on(Node.class).getKey(), keyComparator);
				if (knownClosestNodes.size() >= maxNodes)
					knownClosestNodes.subList(maxNodes, knownClosestNodes.size()).clear();
				
				if (!hasMoreToQuery())
					break;
				
				if (returnedCachedResults != null)
					break;
				
			}
			
		} while (true);
		
		
		knownClosestNodes = Collections.unmodifiableList(knownClosestNodes);
		
		sendStoreResults(lastSentTo);
		
		if (returnedCachedResults != null)
			nrRemoteCacheHits.incrementAndGet();

		synchronized (this) {
			nrQueried = alreadyQueried.size()+querying.size()-1;
		}
		
		return knownClosestNodes;
	}

	
	
	@Override
	public synchronized void completed(KadMessage msg, Node n) {
		notifyAll();
		querying.remove(n);
		alreadyQueried.add(n);
		
		if (returnedCachedResults != null)
			return;
		
		List<Node> nodes = ((FindNodeResponse)msg).getNodes();
		nodes.removeAll(querying);
		nodes.removeAll(alreadyQueried);
		nodes.removeAll(knownClosestNodes);
		
		knownClosestNodes.addAll(nodes);
		
		if (((FindNodeResponse)msg).isCachedResults()) {
			returnedCachedResults = n;
			return;
		}
		
		// listing n as last contacted nodes in the algorithm
		// that did not have the results in its cache
		lastSentTo.add(n);
		if (lastSentTo.size() > nrShare)
			lastSentTo.remove(0);
		
	}
	
	@Override
	public synchronized void failed(Throwable exc, Node n) {
		notifyAll();
		querying.remove(n);
		alreadyQueried.add(n);
		
	}
}
