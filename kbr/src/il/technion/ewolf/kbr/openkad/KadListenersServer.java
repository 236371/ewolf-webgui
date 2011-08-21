package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.NodeConnectionListener;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.ops.KadOperationsExecutor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.name.Named;

class KadListenersServer {

	private final ExecutorService executor;
	private final List<Entry> listeners = Collections.synchronizedList(new ArrayList<Entry>());
	private final List<RPC> listenerServerMessages = Arrays.asList(new RPC[] { RPC.MSG, RPC.CONN, RPC.SOCKET_CONN });
	private final int connPort;
	private final KadOperationsExecutor opExecutor;
	
	private final class Entry {
		private final String pattern;
		private final NodeConnectionListener listener;
		
		Entry(String pattern, NodeConnectionListener listener) {
			this.pattern = pattern;
			this.listener = listener;
		}
		
		public String getPattern() {
			return pattern;
		}
		
		
		boolean matches(String tag) {
			return tag.matches(pattern);
		}
		
		private void onIncomingMessage(KadMessage msg) throws IOException {
			listener.onIncomingMessage(
					msg.getTag(),
					msg.getFirstHop(),
					new ByteArrayInputStream(msg.getConent()));
		}
		
		void onIncomingConnection(KadMessage msg, Socket sock) throws IOException {
			listener.onIncomingConnection(
					msg.getTag(),
					msg.getFirstHop(),
					sock);
		}
	}
	
	
	
	@Inject
	KadListenersServer(
			KadOperationsExecutor opExecutor,
			@Named("kadnet.srv.conn.port") int connPort,
			@Named("kadnet.executors.listeners") ExecutorService executor) {
		this.opExecutor = opExecutor;
		this.executor = executor;
		this.connPort = connPort;
	}
	
	
	public void register(String pattern, NodeConnectionListener listener) {
		synchronized (listeners) {
			listeners.add(new Entry(pattern, listener));
		}
	}
	
	private void executeIncomingMessage(final Entry matchedEntry, final KadMessage msg) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					matchedEntry.onIncomingMessage(msg);
				} catch (Exception e) {
				}
			}
		});
	}
	
	private void executeIncomingConnection(final Entry matchedEntry, final KadMessage msg, final Socket sock) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					matchedEntry.onIncomingConnection(msg, sock);
				} catch (Exception e) {
				}
			}
		});
	}
	
	private void executeIncomingConnection(final Entry matchedEntry, final KadMessage msg, final ServerSocket srvSock) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				Socket sock = null;
				try {
					srvSock.setSoTimeout(1000);
					sock = srvSock.accept();
				} catch (IOException e) {
				} finally {
					try { srvSock.close(); } catch (Exception e) {}
				}
				
				if (sock != null) {
					try {
						matchedEntry.onIncomingConnection(msg, sock);
					} catch (IOException e) {
					}
				}
				
			}
		});
	}
	
	public void incomingListenerMessage(final KadMessage msg, KadMessageBuilder response) throws IOException {
		if (!listenerServerMessages.contains(msg.getRpc()))
			throw new IllegalArgumentException();
		
		Entry matchedEntry = null;
		// search for a matching entry
		synchronized (listeners) {
			for (Entry e : listeners)
				if (e.matches(msg.getTag())) {
					matchedEntry = e;
					break;
				}
		}
		
		if (matchedEntry == null)
			return;
		
		
		switch (msg.getRpc()) {
		case MSG:
			executeIncomingMessage(matchedEntry, msg);
			response.setRpc(RPC.ACK);
			break;
			
		case CONN:
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			matchedEntry.listener.onIncomingMessage(
					msg.getTag(),
					msg.getFirstHop(),
					new ByteArrayInputStream(msg.getConent()),
					out);
			
			response.setRpc(RPC.CONN_RESPONSE)
				.setContent(out.toByteArray());
			break;
			
		case SOCKET_CONN:
			try {
				//System.err.println(msg.getConnPort());
				final Socket sock = new Socket(msg.getFirstHop().getAddress(), msg.getConnPort());
				executeIncomingConnection(matchedEntry, msg, sock);
				response.setRpc(RPC.ACK);
			} catch (Exception e) {
				// error connecting back to the source
				// try to open a server socket and ask the source to connect to me
				ServerSocket srvSock = new ServerSocket(connPort);
				executeIncomingConnection(matchedEntry, msg, srvSock);
				response.setConnPort(srvSock.getLocalPort());
				response.setRpc(RPC.SOCKET_CONN_RESPONSE);
			}
			break;
		}
		
		
	}


	public void unregister(String pattern) {
		synchronized (listeners) {
			Iterator<Entry> itr = listeners.iterator();
			while (itr.hasNext()) {
				Entry entry = itr.next();
				if (entry.getPattern().equals(pattern)) {
					itr.remove();
				}
			}
		}
	}
	
}
