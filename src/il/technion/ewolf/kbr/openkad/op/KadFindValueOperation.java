package il.technion.ewolf.kbr.openkad.op;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyComparator;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.msg.FindNodeRequest;
import il.technion.ewolf.kbr.openkad.msg.FindNodeResponse;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Find value operation where the value is a find node operation results
 * @author eyal.kibbar@gmail.com
 *
 */
public class KadFindValueOperation extends FindValueOperation implements CompletionHandler<KadMessage, Node> {

	// state
	private List<Node> knownClosestNodes;
	private Key key;
	private final Set<Node> alreadyQueried;
	private final Set<Node> querying;
	private int nrQueried;
	private Collection<Node> bootstrap = Collections.emptyList();
	private boolean gotCachedResult = false;
	
	// dependencies
	private final Provider<FindNodeRequest> findNodeRequestProvider;
	private final Provider<MessageDispatcher<Node>> msgDispatcherProvider;
	private final int kBucketSize;
	private final KBuckets kBuckets;
	private final Node localNode;
	private final KadCache cache;
	
	@Inject
	KadFindValueOperation(
			@Named("openkad.local.node") Node localNode,
			@Named("openkad.bucket.kbuckets.maxsize") int kBucketSize,
			Provider<FindNodeRequest> findNodeRequestProvider,
			Provider<MessageDispatcher<Node>> msgDispatcherProvider,
			KBuckets kBuckets,
			KadCache cache) {
		this.localNode = localNode;
		this.kBucketSize = kBucketSize;
		this.kBuckets = kBuckets;
		this.findNodeRequestProvider = findNodeRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.cache = cache;
		
		alreadyQueried = new HashSet<Node>();
		querying = new HashSet<Node>();
	}
	
	public KadFindValueOperation setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public int getNrQueried() {
		return nrQueried;
	}
	
	public KadFindValueOperation setBootstrap(Collection<Node> bootstrap) {
		this.bootstrap = bootstrap;
		return this;
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
			.setKey(key);
		
		msgDispatcherProvider.get()
			.addFilter(new IdMessageFilter(findNodeRequest.getId()))
			.addFilter(new TypeMessageFilter(FindNodeResponse.class))
			.setConsumable(true)
			.setCallback(to, this)
			.send(to, findNodeRequest);
	}
	
	@Override
	public List<Node> doFindValue() {
		List<Node> cacheResults = cache.search(key);
		if (cacheResults != null)
			return cacheResults;
		
		knownClosestNodes = kBuckets.getClosestNodesByKey(key, kBucketSize);
		knownClosestNodes.add(localNode);
		bootstrap.removeAll(knownClosestNodes);
		knownClosestNodes.addAll(bootstrap);
		alreadyQueried.add(localNode);
		KeyComparator keyComparator = new KeyComparator(key);
		
		do {
			synchronized(this) {
				knownClosestNodes = sort(knownClosestNodes, on(Node.class).getKey(), keyComparator);
				if (knownClosestNodes.size() >= kBucketSize)
					knownClosestNodes.subList(kBucketSize, knownClosestNodes.size()).clear();
				
				if (gotCachedResult)
					break;
				
				if (!hasMoreToQuery())
					break;
			}
			
			Node n = takeUnqueried();
			
			if (n != null) {
				sendFindNode(n);
			} else {
				synchronized (this) {
					if (!querying.isEmpty()) {
						try {
							wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
		} while (true);

		knownClosestNodes = Collections.unmodifiableList(knownClosestNodes);
		
		cache.insert(key, knownClosestNodes);
		
		synchronized (this) {
			nrQueried = alreadyQueried.size()-1 + querying.size();
		}
		
		return knownClosestNodes;
	}
	
	@Override
	public synchronized void completed(KadMessage msg, Node n) {
		notifyAll();
		querying.remove(n);
		alreadyQueried.add(n);
		
		if (gotCachedResult)
			return;
		
		List<Node> nodes = ((FindNodeResponse)msg).getNodes();
		nodes.removeAll(querying);
		nodes.removeAll(alreadyQueried);
		nodes.removeAll(knownClosestNodes);
		
		knownClosestNodes.addAll(nodes);
		
		if (((FindNodeResponse)msg).isCachedResults())
			gotCachedResult = true;
		
	}
	
	@Override
	public synchronized void failed(Throwable exc, Node n) {
		notifyAll();
		querying.remove(n);
		alreadyQueried.add(n);
		
	}
}
