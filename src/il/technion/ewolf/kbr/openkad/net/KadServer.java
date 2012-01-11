package il.technion.ewolf.kbr.openkad.net;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.is;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class KadServer implements Runnable {

	// dependencies
	private final KadSerializer serializer;
	private final Provider<DatagramSocket> sockProvider;
	private final BlockingQueue<DatagramPacket> pkts;
	private final ExecutorService srvExecutor;
	private final Set<MessageDispatcher<?>> expecters;
	private final String kadScheme;
	
	private final AtomicInteger nrIncomingMessages;
	@Inject
	KadServer(
			KadSerializer serializer,
			@Named("openkad.scheme.name") String kadScheme,
			@Named("openkad.net.udp.sock") Provider<DatagramSocket> sockProvider,
			@Named("openkad.net.buffer") BlockingQueue<DatagramPacket> pkts,
			@Named("openkad.executors.server") ExecutorService srvExecutor,
			@Named("openkad.net.expecters") Set<MessageDispatcher<?>> expecters,
			@Named("openkad.testing.nrIncomingMessages") AtomicInteger nrIncomingMessages) {
		
		this.kadScheme = kadScheme;
		this.serializer = serializer;
		this.sockProvider = sockProvider;
		this.pkts = pkts;
		this.srvExecutor = srvExecutor;
		this.expecters = expecters;
		this.nrIncomingMessages = nrIncomingMessages;
	}
	
	public void bind() {
		sockProvider.get();
	}
	
	public void send(Node to, KadMessage msg) throws IOException {
		ByteArrayOutputStream bout = null;
		try {
			bout = new ByteArrayOutputStream();
			serializer.write(msg, bout);
			byte[] bytes = bout.toByteArray();
			DatagramPacket pkt = new DatagramPacket(bytes, 0, bytes.length);
			
			pkt.setSocketAddress(to.getSocketAddress(kadScheme));
			sockProvider.get().send(pkt);
		} finally {
			try { bout.close(); } catch (Exception e) {}
		}
	}
	
	@Override
	public void run() {
		runloop();
	}
	
	//private static final AtomicInteger t = new AtomicInteger(0);
	private void handleIncomingPacket(final DatagramPacket pkt) {
		nrIncomingMessages.incrementAndGet();
		srvExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				ByteArrayInputStream bin = null;
				KadMessage msg = null;
				try {
					bin = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
					/*
					if (t.get() < bin.available()) {
						t.set(bin.available());
						System.out.println(t.get());
					}
					*/
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
	
	private void runloop() {
		while (true) {
			DatagramPacket pkt = null;
			try {
				pkt = new DatagramPacket(new byte[1024*64], 1024*64);//pkts.take();
				sockProvider.get().receive(pkt);
				handleIncomingPacket(pkt);
			} catch (Exception e) {
				// insert the taken pkt back
				//if (pkt != null)
				//	pkts.add(pkt);
				
				e.printStackTrace();
			}
		}
	}
	
}
