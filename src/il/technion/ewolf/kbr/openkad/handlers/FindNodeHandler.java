package il.technion.ewolf.kbr.openkad.handlers;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.msg.FindNodeRequest;
import il.technion.ewolf.kbr.openkad.msg.FindNodeResponse;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.net.KadServer;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.MessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Handle find node requests by giving the known closest nodes to the requested key
 * from the KBuckets data structure
 * @author eyal.kibbar@gmail.com
 *
 */
public class FindNodeHandler extends AbstractHandler {

	private final KadServer kadServer;
	private final Node localNode;
	private final KadCache cache;
	private final KBuckets kBuckets;
	private final int kBucketSize;
	
	private final AtomicInteger nrFindnodeHits;
	private final AtomicInteger nrFindnodeMiss;
	
	
	@Inject
	FindNodeHandler(
			Provider<MessageDispatcher<Void>> msgDispatcherProvider,
			KadServer kadServer,
			@Named("openkad.local.node") Node localNode,
			KadCache cache,
			KBuckets kBuckets,
			@Named("openkad.bucket.kbuckets.maxsize") int kBucketSize,
			
			@Named("openkad.testing.nrFindnodeHits") AtomicInteger nrFindnodeHits,
			@Named("openkad.testing.nrFindnodeMiss") AtomicInteger nrFindnodeMiss) {
		
		super(msgDispatcherProvider);
		this.kadServer = kadServer;
		this.localNode = localNode;
		this.cache = cache;
		this.kBuckets = kBuckets;
		this.kBucketSize = kBucketSize;
		
		this.nrFindnodeHits = nrFindnodeHits;
		this.nrFindnodeMiss = nrFindnodeMiss;
	}

	@Override
	public void completed(KadMessage msg, Void attachment) {
		try {
			FindNodeRequest findNodeRequest = ((FindNodeRequest)msg);
			FindNodeResponse findNodeResponse = findNodeRequest
					.generateResponse(localNode)
					.setCachedResults(false);
			
			List<Node> cachedResults = null;
			
			if (!findNodeRequest.shouldSearchCache()) {
				findNodeResponse.setNodes(kBuckets.getClosestNodesByKey(
						findNodeRequest.getKey(), kBucketSize));
				
			} else {
				 // requester ask to search in cache
				cachedResults = cache.search(findNodeRequest.getKey());
				
				if (cachedResults == null) {
					nrFindnodeMiss.incrementAndGet();
					findNodeResponse.setNodes(kBuckets.getClosestNodesByKey(
							findNodeRequest.getKey(), kBucketSize));
				} else {
					nrFindnodeHits.incrementAndGet();
					findNodeResponse
						.setNodes(new ArrayList<Node>(cachedResults))
						.setCachedResults(true);
						
				}
			}
			kadServer.send(msg.getSrc(), findNodeResponse);
		} catch (Exception e) {
			// nothing to do
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// should never b here
	}

	@Override
	protected Collection<MessageFilter> getFilters() {
		// only accept FindNodeRequests messages
		return Arrays.asList(new MessageFilter[] {
				new TypeMessageFilter(FindNodeRequest.class)
		});
	}

}
