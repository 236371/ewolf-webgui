package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.KadConnectionListener;
import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;


import com.google.inject.Inject;
import com.google.inject.name.Named;


public class KadProxyServer implements Runnable {

	private final ExecutorService executor;
	private final Selector selector;
	private KadConnectionListener listener = null;
	private final AtomicLong lastIncomingConnection = new AtomicLong(0);
	private long firewallCheckInterval;
	
	@Inject
	KadProxyServer(
			@Named("kadnet.proxy.checkinterval") long firewallCheckInterval,
			@Named("kadnet.executors.incoming") ExecutorService executor) throws IOException {
		this.executor = executor;
		this.firewallCheckInterval = firewallCheckInterval;
		selector = Selector.open();
		
	}
	
	public void setKadConnectionListener(KadConnectionListener l) {
		this.listener = l;
	}
	
	
	public SelectionKey register(KadConnection conn) throws ClosedChannelException {
		synchronized (selector) {
			if (!(conn.getChannel() instanceof SocketChannel)) {
				throw new IllegalArgumentException("unsuitable kad connection");
			}
			//channels.add(conn.getChannel());
			System.err.println("");
			return conn.getChannel().register(selector, SelectionKey.OP_READ, conn);
		}
	}

	public boolean shouldKeepAlive() {
		return System.currentTimeMillis() - lastIncomingConnection.get() > firewallCheckInterval;
	}
	
	public void preProcess(KadConnection conn, KadMessageBuilder builder) {
		builder.setKeepAlive(true);
	}
	
	public void postProcess(KadConnection conn, KadMessage msg) {
		
	}
	
	public void receivedIncomingConnection() {
		lastIncomingConnection.set(System.currentTimeMillis());
	}
	
	private void executeIncomingMessage(
			final KadConnection conn, 
			final KadMessage incomingMessage) {
		
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					KadMessageBuilder msgBuilder = new KadMessageBuilder();
					listener.onIncomingMessage(incomingMessage, new KadMessageBuilder());
					msgBuilder.sendTo(conn);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				selector.select();
			} catch (IOException e) {
				continue;
			}
			
			synchronized (selector) {
				for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
					SelectionKey key = itr.next();
					itr.remove();
					final KadConnection conn = (KadConnection)key.attachment();
					try {
						executeIncomingMessage(conn, conn.recvMessage());
					} catch (Exception e) {
						conn.close();
						key.cancel();
					}
				}
			}
		}
	}
	
	
	
	
}
