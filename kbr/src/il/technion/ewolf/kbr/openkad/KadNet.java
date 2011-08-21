package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.NodeConnectionListener;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.net.KadConnection;
import il.technion.ewolf.kbr.openkad.net.KadServer;
import il.technion.ewolf.kbr.openkad.ops.KadOperation;
import il.technion.ewolf.kbr.openkad.ops.KadOperationsExecutor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class KadNet implements KeybasedRouting, KadConnectionListener {

	private final KadNode localNode;
	private final KBuckets kbuckets;
	private final KadServer kadServer;
	private final KadOperationsExecutor opExecutor;
	private final KadListenersServer listenersServer;
	private final KeyFactory keyFactory;
	// private final KadProxyServer kadProxyServer;
	// private final OpenedKadConnections openedKadConnections;
	private final KadRefresher kadRefresher;
	private final Logger logger;

	@Inject
	KadNet(
			Logger logger,
			/*@Named("kadnet.logging.level.kadnet")*/ Level lvl,
			@Named("kadnet.srv.buffsize") int buffSize,
			@Named("kadnet.executors.incoming") ExecutorService executor,
			@Named("kadnet.localnode") KadNode localNode,
			KeyFactory keyFactory, KBuckets kbuckets, KadServer kadServer,
			KadOperationsExecutor opExecutor,
			// KadProxyServer kadProxyServer,
			// OpenedKadConnections openedKadConnections,
			KadListenersServer listenersServer, KadRefresher kadRefresher)
			throws IOException {
		this.logger = logger;
		this.logger.setLevel(lvl);
		this.keyFactory = keyFactory;

		this.localNode = localNode;
		this.kbuckets = kbuckets;

		this.kadServer = kadServer;
		this.kadServer.setKadConnectionListener(this);

		// this.kadProxyServer = kadProxyServer;
		// this.kadProxyServer.setKadConnectionListener(this);
		// this.openedKadConnections = openedKadConnections;

		this.opExecutor = opExecutor;
		this.listenersServer = listenersServer;
		this.kadRefresher = kadRefresher;

	}

	KadOperationsExecutor getOpExecutor() {
		return opExecutor;
	}
	
	@Override
	public void shutdown() {
		kadRefresher.shutdown();
		kadServer.shutdown();
	}

	@Override
	public void create() throws IOException {
		for (KadEndpoint endpoint : localNode.getEndpoints()) {

			kadServer.register(endpoint.getKadProtocol(),
					new InetSocketAddress(endpoint.getPort()));

		}
		new Thread(kadServer).start();
	}

	@Override
	public Future<Void> join(URI bootstrap) {
		Future<Void> $ = opExecutor.submitJoinOperation(bootstrap);
		new Thread(kadRefresher).start();
		return $;
	}

	@Override
	public Future<List<Node>> findNodes(Key key, int n) {
		final KadOperation<List<KadNode>> op = opExecutor
				.createNodeLookupOperation(key, n);
		return opExecutor.submit(new Callable<List<Node>>() {

			@Override
			public List<Node> call() throws Exception {
				List<Node> $ = new ArrayList<Node>();
				$.addAll(op.call());
				return $;
				/*
				return extract(op.call(), on(KadNode.class).getNode(opExecutor));
				List<Node> $ = new ArrayList<Node>();
				for (KadNode n : op.call()) {
					$.add(n.getNode(opExecutor));
				}
				return $;
				*/
			}
		});
	}

	@Override
	public Set<Node> getNeighbors() {
		Set<Node> $ = new HashSet<Node>();
		$.addAll(kbuckets.getAllNodes());
		return $;
	}

	@Override
	public void register(String pattern, NodeConnectionListener listener) {
		listenersServer.register(pattern, listener);
	}
	
	@Override
	public void unregister(String pattern) {
		listenersServer.unregister(pattern);
	}

	@Override
	public void onIncomingConnection(KadConnection conn) throws IOException {
		//boolean keepalive = false;
		// kadProxyServer.receivedIncomingConnection();

		try {
			KadMessage msg = conn.recvMessage();
			KadMessageBuilder builder = new KadMessageBuilder();
			onIncomingMessage(msg, builder);
			/*
			 * if (openedKadConnections.keepAlive(conn, msg)) { keepalive =
			 * true;
			 * System.err.println(localNode+": keeping connection alive with "
			 * +msg.getLastHop()); builder.setKeepAlive(true); }
			 */

			builder.sendTo(conn);

		} finally {
			//if (!keepalive)
				conn.close();
		}
	}

	private void forward(KadMessage msg, KadMessageBuilder response)
			throws IOException {
		throw new UnsupportedOperationException();
		/*
		 * try { KadConnection conn = openedKadConnections.get(msg.getDst()); if
		 * (conn == null) throw new
		 * IOException("not connected to "+msg.getDst());
		 * 
		 * new KadMessageBuilder(msg) .addHop(localNode) .setKeepAlive(false)
		 * .sendTo(conn);
		 * 
		 * response .loadKadMessage(conn.recvMessage()) .setKeepAlive(false)
		 * .addHop(localNode);
		 * 
		 * } catch (Exception e) { openedKadConnections.remove(msg.getDst());
		 * throw new IOException(e); }
		 */
	}

	@Override
	public void onIncomingMessage(KadMessage msg, KadMessageBuilder response)
			throws IOException {

		logger.info("message recved, rpc = "+msg.getRpc()+" route = "+msg.getPath());
		
		opExecutor.executeInsertNodeOperation(msg.getLastHop());

		if (msg.getDst() != null && !localNode.getKey().equals(msg.getDst())) {
			forward(msg, response);
			return;
		}

		response.addHop(localNode);

		switch (msg.getRpc()) {
		case PONG: case FIND_NODE_RESPONSE: case CONN_RESPONSE: case MSG_RESPONSE:
			logger.warning("recved response while expecting requests only");
			throw new IllegalArgumentException();
			
		case PING:
			response.setRpc(RPC.PONG);
			break;
			
		case FIND_NODE:
			Set<Key> exclude = new HashSet<Key>();
			exclude.add(msg.getFirstHop().getKey());
			
			// direct connection, no need to add myself
			if (msg.getFirstHop().equals(msg.getLastHop())) 
				exclude.add(localNode.getKey());

			response.setRpc(RPC.FIND_NODE_RESPONSE)
					.addNodes(kbuckets.getKClosestNodes(msg.getKey(), exclude, msg.getMaxNodeCount()))
					.setKey(msg.getKey());
			break;

		case MSG: case CONN: case SOCKET_CONN:
			listenersServer.incomingListenerMessage(msg, response);
			break;
		}
	}

	@Override
	public KeyFactory getKeyFactory() {
		return keyFactory;
	}

	@Override
	public String toString() {
		return localNode.toString() + "\n======\n" + kbuckets.toString();
	}

	@Override
	public KadNode getLocalNode() {
		return localNode;
	}

	
	@Override
	public Future<Socket> openConnection(Node to, String tag)
			throws IOException {
			if (!(to instanceof KadNode))
				throw new IllegalArgumentException("invalid node");
			
			KadNode n = (KadNode)to;
			
			if (!keyFactory.isValid(n.getKey()))
				throw new IllegalArgumentException("invalid key");
			
			return opExecutor.submitOpenConnectionOperation(n, tag);
	}

	@Override
	public OutputStream sendMessage(Node to, String tag) throws IOException {
		if (!(to instanceof KadNode))
			throw new IllegalArgumentException("invalid node");
		
		KadNode n = (KadNode)to;
		
		if (!keyFactory.isValid(n.getKey()))
			throw new IllegalArgumentException("invalid key");
		
		return new MessageOutputStream(getLocalNode(), n, tag);
	}

}
