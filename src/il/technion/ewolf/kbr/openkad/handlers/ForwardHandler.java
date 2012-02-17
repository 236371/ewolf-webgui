package il.technion.ewolf.kbr.openkad.handlers;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.cache.KadCache;
import il.technion.ewolf.kbr.openkad.msg.ForwardMessage;
import il.technion.ewolf.kbr.openkad.msg.ForwardRequest;
import il.technion.ewolf.kbr.openkad.msg.ForwardResponse;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.net.KadServer;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.IdMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.MessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;
import il.technion.ewolf.kbr.openkad.op.FindValueOperation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Handle forward requests according to the forward algorithm
 * TODO: link for published article
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class ForwardHandler extends AbstractHandler {

	private final KadCache cache;
	private final KadServer kadServer;
	private final KBuckets kBuckets;
	
	private final Provider<FindValueOperation> findValueOperationProvider;
	private final Provider<ForwardRequest> forwardRequestProvider;
	private final Provider<MessageDispatcher<Void>> msgDispatcherProvider;
	
	private final Node localNode;
	private final int myColor;
	private final int nrColors;
	private final int kBucketSize;
	private final long timeout;
	
	private final ExecutorService opExecutor;
	private final ExecutorService forwardExecutor;
	
	//testing
	private final AtomicInteger nrFindNodesWithWrongColor;
	private final AtomicInteger nrForwardHandling;
	private final AtomicInteger nrForwardHandlingFromInitiator;
	
	@Inject
	ForwardHandler(
			KadCache cache,
			KadServer kadServer,
			KBuckets kBuckets,
			
			@Named("openkad.op.lastFindValue") Provider<FindValueOperation> findValueOperationProvider,
			Provider<ForwardRequest> forwardRequestProvider,
			Provider<MessageDispatcher<Void>> msgDispatcherProvider,
			
			@Named("openkad.local.node") Node localNode,
			@Named("openkad.local.color") int myColor,
			@Named("openkad.color.nrcolors") int nrColors,
			@Named("openkad.bucket.kbuckets.maxsize") int kBucketSize,
			@Named("openkad.net.forwarded.timeout") long timeout,
			
			@Named("openkad.executors.op") ExecutorService opExecutor,
			@Named("openkad.executors.forward") ExecutorService forwardExecutor,
			
			//testing
			@Named("openkad.testing.nrFindNodesWithWrongColor") AtomicInteger nrFindNodesWithWrongColor,
			@Named("openkad.testing.nrForwardHandling") AtomicInteger nrForwardHandling,
			@Named("openkad.testing.nrForwardHandlingFromInitiator") AtomicInteger nrForwardHandlingFromInitiator) {
		
		super(msgDispatcherProvider);
		this.cache = cache;
		this.kadServer = kadServer;
		this.kBuckets = kBuckets;
		
		this.findValueOperationProvider = findValueOperationProvider;
		this.forwardRequestProvider = forwardRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		
		this.localNode = localNode;
		this.myColor = myColor;
		this.nrColors = nrColors;
		this.kBucketSize = kBucketSize;
		this.timeout = timeout;
		
		this.opExecutor = opExecutor;
		this.forwardExecutor = forwardExecutor;
		
		this.nrFindNodesWithWrongColor = nrFindNodesWithWrongColor;
		this.nrForwardHandling = nrForwardHandling;
		this.nrForwardHandlingFromInitiator = nrForwardHandlingFromInitiator;
	}

	private void doFindValue(final ForwardRequest req) throws RejectedExecutionException {
		opExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				if (myColor != req.getKey().getColor(nrColors)) {
					nrFindNodesWithWrongColor.incrementAndGet();
				}
				
				System.out.println(localNode+": doing the find node");
				FindValueOperation op = findValueOperationProvider.get()
					.setBootstrap(req.getBootstrap())
					.setKey(req.getKey());
				
				List<Node> results = op.doFindValue();
				
				System.out.println(localNode+": finished find node, returning results");
				
				ForwardMessage msg = req.generateMessage(localNode)
					.setFindNodeHops(op.getNrQueried())
					.setPathLength(0)
					.setNodes(results);
				
				System.out.println(localNode+": sending "+results+" back to "+req.getSrc());
				
				try {
					kadServer.send(req.getSrc(), msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
	@Override
	public void completed(KadMessage msg, Void nothing) {
		final ForwardRequest req = (ForwardRequest)msg;
		
		nrForwardHandling.incrementAndGet();
		if (req.isInitiator()) // TODO: remove before publish
			nrForwardHandlingFromInitiator.incrementAndGet();
		
		System.out.println(localNode+": recved forward request from "+req.getSrc());
		List<Node> cachedResults = cache.search(req.getKey());
		ForwardResponse res = req.generateResponse(localNode);
		
		try {
			if (cachedResults != null) {
				System.out.println(localNode+": cache hit !");
				kadServer.send(req.getSrc(), res.setNodes(cachedResults));
				return;
			}
			// no cached result, send
			// continue with the operation
		} catch (IOException e) {
			// could not send back the results
			// nothing to do
			return;
		}
		
		
		assert (cachedResults == null);
		System.out.println(localNode+": result was not in cache");
		// result was not in cache
		// either forward to someone else or do the job myself
		try {
			if (myColor == req.getKey().getColor(nrColors)) {
				// i need to preform the find node because i have
				// the right color
				System.out.println(localNode+": I have the right color");
				doFindValue(req);
			} else {
				// i am in the wrong color, forward the request to
				// someone else, preferably with the correct color
				doForward(req);
			}
			
			System.out.println(localNode+": sending ack back");
			kadServer.send(req.getSrc(), res
					.setNodes(null)
					.setAck());
			
		} catch (RejectedExecutionException e) {
			// overloaded, could not handle the request,
			// notify the requester
			try {
				// tell the source I was unable to fulfill its request
				kadServer.send(req.getSrc(), res
						.setNodes(null)
						.setNack());
						
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// could not send the ack back to the requester
			// nothing to do
		}
	}
	
	
	
	private MessageDispatcher<Void> generateExpectDispatcher(
			final ForwardRequest incomingReq,
			final ForwardRequest outgoingReq) {
		
		return msgDispatcherProvider.get()
			.setConsumable(true)
			.addFilter(new IdMessageFilter(outgoingReq.getId()))
			.addFilter(new TypeMessageFilter(ForwardMessage.class))
			.setTimeout(timeout, TimeUnit.MILLISECONDS)
			.setCallback(null, new CompletionHandler<KadMessage, Void>() {
				
				@Override
				public void failed(Throwable exc, Void nothing) {
					// nothing to do
				}
				
				@Override
				public void completed(KadMessage msg, Void nothing) {
					// forward back to src
					ForwardMessage res = (ForwardMessage)msg;
					System.out.println(localNode+": "+res.getSrc()+" had an answer: "+res.getNodes());
					try {
						kadServer.send(incomingReq.getSrc(), incomingReq
								.generateMessage(localNode)
								.setFindNodeHops(res.getFindNodeHops())
								.setPathLength(1+res.getPathLength())
								.setNodes(res.getNodes()));
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).register();
	}
	
	private MessageDispatcher<Void> generateOutgoingDispatcher(
			final ForwardRequest incomingReq,
			final ForwardRequest outgoingReq,
			final MessageDispatcher<Void> expectDispatcher) {
		
		
		return msgDispatcherProvider.get()
			.setConsumable(true)
			.addFilter(new IdMessageFilter(outgoingReq.getId()))
			.addFilter(new TypeMessageFilter(ForwardResponse.class))
			.setCallback(null,  new CompletionHandler<KadMessage, Void>() {
				
				@Override
				public void failed(Throwable exc, Void nothing) {
					// no need to wait for result, it will
					// never arrive
					expectDispatcher.cancel(exc);
					
					// notify the requester that his request
					// had failed to be handled
					System.out.println(localNode+": did not hear from the remote node");
					try {
						kadServer.send(incomingReq.getSrc(), incomingReq
								.generateMessage(localNode)
								.setNodes(null));
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void completed(KadMessage msg, Void nothing) {
					// we got the response
					ForwardResponse res = (ForwardResponse)msg;
					if (res.getNodes() == null) {
						System.out.println(localNode+": remote node is calculating for me");
						// next hop got the request and is resolving it
						// an answer should arrive soon to the expectMessage
						// handler
						return;
					}
					
					// the remote node had an answer !!
					System.out.println(localNode+": remote node had an answer in its cache");
					// no need to expect a message, we already got the results
					expectDispatcher.cancel(new CancellationException());
					
					// return it to the requester
					try {
						kadServer.send(incomingReq.getSrc(), incomingReq
								.generateMessage(localNode)
								.setPathLength(0)
								.setNodes(res.getNodes()));
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
	}
	
	private void doForward(final ForwardRequest incomingReq) throws RejectedExecutionException {
		// forward the request to someone else
		System.out.println(localNode+": I dont have the right color");
		List<Node> myBootstrap = kBuckets.getAllFromBucket(incomingReq.getKey());
		if (myBootstrap.isEmpty()) {
			// if we cannot continue advancing to target, do the find node myself
			System.out.println(localNode+": could not adavnce to target, do the find node myself");
			doFindValue(incomingReq);
			return;
		}
		
		final ForwardRequest outgoingReq = forwardRequestProvider.get()
				.setPreviousRequest(incomingReq)
				.mergeBootstraps(myBootstrap, kBucketSize);
		
		final Node nextHop = outgoingReq.calcNextHop(nrColors);
		if (nextHop == null) {
			System.out.println(localNode+": no suitable next hop was found");
			doFindValue(incomingReq);
			return;
		}
		
		System.out.println(localNode+": forwarding to "+nextHop);
		
		// expect a message from nextHop
		final MessageDispatcher<Void> expectMessage = generateExpectDispatcher(incomingReq, outgoingReq);
		
		try {
			// send a request to next hop
			// if next hop returns a result, the expect message
			// will be canceled and the result will be sent back
			// to the requester
			// if the next hop only returns an ACK, we will continue
			// waiting for the result in the expect message.
			forwardExecutor.execute(new Runnable() {
				
				@Override
				public void run() {
					generateOutgoingDispatcher(incomingReq, outgoingReq, expectMessage)
						.send(nextHop, outgoingReq);
				}
				
			});
			
		} catch (RejectedExecutionException e) {
			expectMessage.cancel(e);
			throw e;
		}
	}

	@Override
	public void failed(Throwable exc, Void nothing) {
		// should never b here
	}

	@Override
	protected Collection<MessageFilter> getFilters() {
		return Arrays.asList(new MessageFilter[] {
				new TypeMessageFilter(ForwardRequest.class)
		});
	}

}
