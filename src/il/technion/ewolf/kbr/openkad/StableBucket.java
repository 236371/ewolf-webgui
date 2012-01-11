package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.PingRequest;
import il.technion.ewolf.kbr.openkad.msg.PingResponse;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class StableBucket implements Bucket {

	// state
	private final List<KadNode> bucket;
	
	//dependencies
	private final int maxSize;
	private final long validTimespan;
	private final Provider<PingRequest> pingRequestProvider;
	private final Provider<MessageDispatcher<Void>> msgDispatcherProvider;
	private final ExecutorService pingExecutor;
	
	@Inject
	StableBucket(
			int maxSize,
			@Named("openkad.bucket.valid_timespan") long validTimespan,
			@Named("openkad.executors.ping") ExecutorService pingExecutor,
			Provider<PingRequest> pingRequestProvider,
			Provider<MessageDispatcher<Void>> msgDispatcherProvider) {
		
		this.maxSize = maxSize;
		this.bucket = new LinkedList<KadNode>();
		this.validTimespan = validTimespan;
		this.pingExecutor = pingExecutor;
		this.pingRequestProvider = pingRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
	}
	
	@Override
	public void insert(final KadNode n) {
		KadNode inBucketReplaceCandidate = null;
		synchronized (bucket) {
			int i = bucket.indexOf(n);
			if (i != -1) { // found node in bucket
				// if heard from n (it is possible to insert n i never had
				// contact with simply by hearing about from another node)
				if (bucket.get(i).getLastContact() < n.getLastContact()) {
					KadNode s = bucket.remove(i);
					s.setNodeWasContacted(n.getLastContact());
					bucket.add(s);
				}
				return;
			} else if (bucket.size() < maxSize) {
				// not found in bucket and there is enough room for n
				bucket.add(n);
				return;
			}
			// n is not in bucket and bucket is full
			inBucketReplaceCandidate = bucket.get(0);
			
			// the first node was only inserted indirectly (meaning, I never recved
			// a msg from it !) and I did recv a msg from n.
			if (inBucketReplaceCandidate.hasNeverContacted() && n.hasContacted()) {
				bucket.remove(inBucketReplaceCandidate);
				bucket.add(n);
				return;
			}
		}
		
		// dont bother to insert n if I never recved a msg from it
		// or if the first node's ping is still valid
		if (n.hasNeverContacted() || inBucketReplaceCandidate.isPingStillValid(validTimespan))
			return;

		
		// both n and firstInBucket have been seen and are competing
		// on place in bucket
		
		// find a node to ping that no one else is currently pinging
		for (int j=0; j < bucket.size(); ++j) {
			if (bucket.get(j).lockForPing()) {
				sendPing(bucket.get(j), n);
			}
		}
		
					
		
		
		
	}
	
	private void sendPing(final KadNode inBucket, final KadNode replaceIfFailed) {
		
		final PingRequest pingRequest = pingRequestProvider.get();
		
		final MessageDispatcher<Void> dispatcher = msgDispatcherProvider.get()
			.setConsumable(true)
			.addFilter(new IdMessageFilter(pingRequest.getId()))
			.addFilter(new TypeMessageFilter(PingResponse.class))
			.setCallback(null, new CompletionHandler<KadMessage, Void>() {
				@Override
				public void completed(KadMessage msg, Void nothing) {
					// ping was recved
					inBucket.setNodeWasContacted();
					inBucket.releasePingLock();
					synchronized (bucket) {
						if (bucket.remove(inBucket)) {
							bucket.add(inBucket);
						}
					}
				}
				@Override
				public void failed(Throwable exc, Void nothing) {
					// ping was not recved
					synchronized (bucket) {
						if (bucket.remove(inBucket)) {
							bucket.add(replaceIfFailed);
						}
					}
					inBucket.releasePingLock();
				}
			});
		
		
		try {
			pingExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
					dispatcher.send(inBucket.getNode(), pingRequest);
				}
			});
		} catch (Exception e) {
			inBucket.releasePingLock();
		}
	}
	
	@Override
	public void addNodesTo(Collection<Node> c) {
		synchronized (bucket) {
			for (KadNode n : bucket) {
				c.add(n.getNode());
			}
		}
	}
	
	@Override
	public String toString() {
		String $;
		synchronized (bucket) {
			$ = bucket.toString();
		}
		return $;
	}
	
}
