package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.KadConnectionListener;
import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class KadServer implements Runnable {

	private final Logger logger;
	private final Selector selector; 
	private final ExecutorService executor;
	private final int buffSize;
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final Collection<SelectableChannel> channels = new HashSet<SelectableChannel>();
	private KadConnectionListener listener;
	
	@Inject
	KadServer(
			Logger logger,
			@Named("kadnet.srv.buffsize") int buffSize,
			@Named("kadnet.executors.incoming") ExecutorService executor) throws IOException {
		selector = Selector.open();
		this.buffSize = buffSize;
		this.executor = executor;
		this.logger = logger;
	}
	
	public void setKadConnectionListener(KadConnectionListener l) {
		this.listener = l;
	}
	
	
	public SelectionKey register(KadProtocol protocol, InetSocketAddress addr) throws IOException {
		SelectableChannel chan = protocol.openChannel(addr);
		if (!(chan instanceof ServerSocketChannel) && !(chan instanceof DatagramChannel))
			throw new UnsupportedOperationException();
		
		channels.add(chan);
		synchronized (selector) {
			if ((chan.validOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT)
				return chan.register(selector, SelectionKey.OP_ACCEPT, protocol);
			
			if ((chan.validOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ)
				return chan.register(selector, SelectionKey.OP_READ, protocol);
			
			throw new UnsupportedOperationException();
		}
	}
	
	public SelectionKey register(KadConnection conn, KadConnectionListener listener) throws ClosedChannelException {
		synchronized (selector) {
			if (!(conn.getChannel() instanceof SocketChannel)) {
				throw new IllegalArgumentException("unsuitable kad connection");
			}
			channels.add(conn.getChannel());
			return conn.getChannel().register(selector, SelectionKey.OP_READ, conn.getProtocol());
		}
	}
	
	private void executeIncomingConnection(
			final KadConnection conn) {
		
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					listener.onIncomingConnection(conn);
				} catch (Exception e) {
				}
			}
		});
	}
	
	private void executeIncomingMessage(
			final KadProtocol protocol,
			final InetSocketAddress remoteAddr,
			final DatagramChannel chan,
			final KadMessage incomingMessage) {
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					KadMessageBuilder msgBuilder = new KadMessageBuilder();
					listener.onIncomingMessage(incomingMessage, msgBuilder);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					protocol.getSerializer().writeKadMessage(msgBuilder.build(), out);
					out.close();
				
					chan.send(ByteBuffer.wrap(out.toByteArray()), remoteAddr);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
	}
	
	private void handleIO(KadProtocol protocol, ServerSocketChannel chan) throws IOException {
		SocketChannel sockChan = chan.accept();
		logger.info("recving message from "+
				sockChan.socket().getRemoteSocketAddress()+
				" in socket channel port "+
				chan.socket().getLocalPort());
		
		sockChan.configureBlocking(false);
		executeIncomingConnection(new KadConnection(sockChan, protocol));
	}
	
	private void handleIO(KadProtocol protocol, DatagramChannel chan) throws IOException, ClassNotFoundException {
		ByteBuffer buff = ByteBuffer.allocate(buffSize);
		InetSocketAddress remoteAddr = (InetSocketAddress)chan.receive(buff);
		logger.info("recving message from "+remoteAddr+" in datagram channel port "+chan.socket().getLocalPort());
		ByteArrayInputStream in = new ByteArrayInputStream(buff.array(), 0, buff.position());
		KadMessage incomingMessage = protocol.getSerializer().readKadMessage(remoteAddr.getAddress(), in);
		
		in.close();
		executeIncomingMessage(
				protocol,
				remoteAddr,
				chan,
				incomingMessage);
	}
	
	
	public void run() {
		
		active.set(true);

		while (active.get()) {
			try {
				// block until we have an event
				selector.select();
				
				synchronized (selector) {
					for (Iterator<SelectionKey> itr = selector.selectedKeys().iterator(); itr.hasNext();) {
						SelectionKey key = itr.next();
						itr.remove();
						KadProtocol protocol = (KadProtocol)key.attachment();
						
						try {
							if (key.channel() instanceof ServerSocketChannel)
								handleIO(protocol, (ServerSocketChannel)key.channel());
							
							else if (key.channel() instanceof DatagramChannel)
								handleIO(protocol, (DatagramChannel)key.channel());
								
						} catch (Exception e) {
							logger.warning("failed to recv message from");
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
		for (SelectableChannel chan : channels) {
			try {
				chan.close();
			} catch (Exception e) {
			}
		}
	}
	
	public void shutdown() {
		active.set(false);
		try {
			selector.close();
		} catch (Exception e) {
			
		}
	}
}
