package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.concurrent.FutureCallback;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.KadRequest;
import il.technion.ewolf.kbr.openkad.net.filter.MessageFilter;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Handle all the messages different states.
 * A request state: 
 * init -> sent -> response received -> callback invoked
 * 
 * A message state:
 * init -> expecting -> message received -> callback invoked -> back to expecting or end
 * 
 * @author eyal.kibbar@gmail.com
 *
 * @param <A>
 */
public class MessageDispatcher<A> {

	// state
	private A attachment;
	private CompletionHandler<KadMessage, A> callback;
	private boolean isConsumbale = true;
	private long timeout;
	private final Set<MessageFilter> filters = new HashSet<MessageFilter>();
	private TimerTask timeoutTimerTask = null;
	private final AtomicBoolean isDone;
	
	// dependencies
	private final BlockingQueue<MessageDispatcher<?>> outstandingRequests;
	private final Set<MessageDispatcher<?>> expecters; // must be sync'ed set
	private final Timer timer;
	private final KadServer communicator;

	
	@Inject
	MessageDispatcher(
			@Named("openkad.net.req_queue") BlockingQueue<MessageDispatcher<?>> outstandingRequests,
			@Named("openkad.net.expecters") Set<MessageDispatcher<?>> expecters,
			@Named("openkad.timer") Timer timer,
			@Named("openkad.net.timeout") long timeout,
			KadServer communicator) {
		
		this.outstandingRequests = outstandingRequests;
		this.expecters = expecters;
		this.timer = timer;
		this.timeout = timeout;
		this.communicator = communicator;
		this.isDone = new AtomicBoolean(false);
	}
	
	
	
	public void cancel(Throwable exc) {
		if (!isDone.compareAndSet(false, true))
			return;
		
		if (timeoutTimerTask != null)
			timeoutTimerTask.cancel();
		
		outstandingRequests.remove(this);
		expecters.remove(this);
		
		if (callback != null)
			callback.failed(exc, attachment);
	}
	
	// returns true if should be handled
	boolean shouldHandleMessage(KadMessage m) {
		for (MessageFilter filter : filters) {
			if (!filter.shouldHandle(m))
				return false;
		}
		return true;
	}
	
	void handle(KadMessage msg) {
		assert (shouldHandleMessage(msg));
		
		if (isDone.get())
			return;
		
		if (timeoutTimerTask != null)
			timeoutTimerTask.cancel();
		
		outstandingRequests.remove(this);
		if (isConsumbale) {
			expecters.remove(this);
			if (!isDone.compareAndSet(false, true))
				return;
		}
		
		if (callback != null)
			callback.completed(msg, attachment);
	}
	
	public MessageDispatcher<A> addFilter(MessageFilter filter) {
		filters.add(filter);
		return this;
	}
	
	public MessageDispatcher<A> setCallback(A attachment, CompletionHandler<KadMessage, A> callback) {
		this.callback = callback;
		this.attachment = attachment;
		return this;
	}
	
	public MessageDispatcher<A> setTimeout(long t, TimeUnit unit) {
		timeout = unit.toMillis(t);
		return this;
	}
	
	public MessageDispatcher<A> setConsumable(boolean consume) {
		isConsumbale = consume;
		return this;
	}
	
	public MessageDispatcher<A> register() {
		expecters.add(this);
		setupTimeout();
		return this;
	}
	
	
	public Future<KadMessage> futureRegister() {
		
		FutureCallback<KadMessage, A> f = new FutureCallback<KadMessage, A>();
		setCallback(null, f);
		expecters.add(this);
		setupTimeout();
		
		return f;
	}
	
	private void setupTimeout() {
		if (!isConsumbale)
			return;
		
		timeoutTimerTask = new TimerTask() {
			
			@Override
			public void run() {
				MessageDispatcher.this.cancel(new TimeoutException());
			}
		};
		timer.schedule(timeoutTimerTask, timeout);
	}
	
	public void send(Node to, KadRequest req) {
		setConsumable(true);
		try {
			/*
			if (!outstandingRequests.offer(this, timeout, TimeUnit.MILLISECONDS))
				throw new RejectedExecutionException();
			*/
			outstandingRequests.put(this);
			expecters.add(this);
			communicator.send(to, req);
			
			setupTimeout();
			
		} catch (Exception e) {
			cancel(e);
		}
	}
	
	public Future<KadMessage> futureSend(Node to, KadRequest req) {
		
		FutureCallback<KadMessage, A> f = new FutureCallback<KadMessage, A>();
		setCallback(null, f);
		
		send(to, req);
		
		return f;
	}
}
