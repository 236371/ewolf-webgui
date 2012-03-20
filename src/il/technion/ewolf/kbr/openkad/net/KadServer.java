package il.technion.ewolf.kbr.openkad.net;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.is;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.PingRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Low level communication handler.
 * This class does all the serialze/de-serialze and socket programming.
 * @author eyal.kibbar@gmail.com
 *
 */
public class KadServer implements Runnable {

	// dependencies
	private final KadSerializer serializer;
	private final Provider<DatagramSocket> sockProvider;
	//private final BlockingQueue<DatagramPacket> pkts;
	private final ExecutorService srvExecutor;
	private final Set<MessageDispatcher<?>> expecters;
	private final String kadScheme;
	
	// testing
	private final AtomicInteger nrOutgoingPings;
	private final AtomicInteger nrIncomingMessages;
	private final AtomicLong nrBytesSent;
	private final AtomicLong nrBytesRecved;
	
	
	// state
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	
	@Inject
	KadServer(
			KadSerializer serializer,
			@Named("openkad.scheme.name") String kadScheme,
			@Named("openkad.net.udp.sock") Provider<DatagramSocket> sockProvider,
			//@Named("openkad.net.buffer") BlockingQueue<DatagramPacket> pkts,
			@Named("openkad.executors.server") ExecutorService srvExecutor,
			@Named("openkad.net.expecters") Set<MessageDispatcher<?>> expecters,
			
			// testing
			@Named("openkad.testing.nrOutgoingPings") AtomicInteger nrOutgoingPings,
			@Named("openkad.testing.nrIncomingMessages") AtomicInteger nrIncomingMessages,
			@Named("openkad.testing.nrBytesSent") AtomicLong nrBytesSent,
			@Named("openkad.testing.nrBytesRecved") AtomicLong nrBytesRecved) {
		
		this.kadScheme = kadScheme;
		this.serializer = serializer;
		this.sockProvider = sockProvider;
		//this.pkts = pkts;
		this.srvExecutor = srvExecutor;
		this.expecters = expecters;
		
		this.nrOutgoingPings = nrOutgoingPings;
		this.nrIncomingMessages = nrIncomingMessages;
		this.nrBytesSent = nrBytesSent;
		this.nrBytesRecved = nrBytesRecved;
	}
	
	/**
	 * Binds the socket
	 */
	public void bind() {
		sockProvider.get();
	}
	
	/**
	 * Sends a message
	 * 
	 * @param to the destination node
	 * @param msg the message to be sent
	 * @throws IOException any socket exception
	 */
	public void send(Node to, KadMessage msg) throws IOException {
		if (msg instanceof PingRequest) {
			nrOutgoingPings.incrementAndGet();
		}
		
		ByteArrayOutputStream bout = null;
		try {
			bout = new ByteArrayOutputStream();
			serializer.write(msg, bout);
			byte[] bytes = bout.toByteArray();
			nrBytesSent.addAndGet(bytes.length);
			DatagramPacket pkt = new DatagramPacket(bytes, 0, bytes.length);
			
			pkt.setSocketAddress(to.getSocketAddress(kadScheme));
			sockProvider.get().send(pkt);
		} finally {
			try { bout.close(); } catch (Exception e) {}
		}
	}
	
	private void handleIncomingPacket(final DatagramPacket pkt) {
		nrIncomingMessages.incrementAndGet();
		nrBytesRecved.addAndGet(pkt.getLength());
		srvExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				ByteArrayInputStream bin = null;
				KadMessage msg = null;
				try {
					bin = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
					msg = serializer.read(bin);
					// fix incoming src address
					msg.getSrc().setInetAddress(pkt.getAddress());
				} catch (Exception e) {
					e.printStackTrace();
					return;
				} finally {
					try { bin.close(); } catch (Exception e) {}
					//pkts.add(pkt);
				}
				
				// call all the expecters
				List<MessageDispatcher<?>> shouldHandle;
				synchronized (expecters) {
					shouldHandle = filter(
							having(on(MessageDispatcher.class).shouldHandleMessage(msg), is(true)),
							expecters);
				}
				for (MessageDispatcher<?> m : shouldHandle) {
					try {
						m.handle(msg);
					} catch (Exception e) {
						// handle fail should not interrupt other handlers
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	/**
	 * The server loop:
	 * 1. accept a message from socket
	 * 2. parse message
	 * 3. handle the message in a thread pool
	 */
	@Override
	public void run() {
		isActive.set(true);
		while (isActive.get()) {
			DatagramPacket pkt = null;
			try {
				pkt = new DatagramPacket(new byte[1024*64], 1024*64);//pkts.take();
				sockProvider.get().receive(pkt);
				handleIncomingPacket(pkt);
			} catch (Exception e) {
				// insert the taken pkt back
				//if (pkt != null)
				//	pkts.add(pkt);
				
				//e.printStackTrace();
			}
		}
	}

	/**
	 * Shutdown the server and closes the socket
	 * @param kadServerThread
	 */
	public void shutdown(Thread kadServerThread) {
		isActive.set(false);
		sockProvider.get().close();
		kadServerThread.interrupt();
		try {
			kadServerThread.join();
		} catch (InterruptedException e) {
		}
	}
	
}
