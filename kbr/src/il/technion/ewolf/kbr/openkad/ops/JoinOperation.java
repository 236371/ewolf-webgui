package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadEndpoint;
import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;


class JoinOperation extends KadOperation<Void> {

	private final KadNode localNode;
	private final KBuckets kbuckets;
	private final KadOperationsExecutor opExecutor;
	private final URI bootstrap;
	//private final KadProxyServer proxyServer;

	JoinOperation(
			Logger logger,
			KadNode localNode,
			KBuckets kbuckets,
			KadOperationsExecutor opExecutor,
			URI bootstrap
			/*KadProxyServer proxyServer*/) {
		super(logger);
		this.localNode = localNode;
		this.kbuckets = kbuckets;
		this.opExecutor = opExecutor;
		this.bootstrap = bootstrap;
		//this.proxyServer = proxyServer;
	}
	
	
	@Override
	public Void call() throws Exception {
		logger.info("starting join operation");
		logger.info("contact the bootstrap node and find its key");
		KadConnection conn = null;
		boolean keepalive = false;
		try {
			conn = new KadEndpoint(bootstrap.getScheme(), bootstrap.getPort())
				.openConnection(InetAddress.getByName(bootstrap.getHost()));
			
			KadMessageBuilder builder = new KadMessageBuilder()
				.addHop(localNode)
				.setRpc(RPC.PING);
			
			//if (conn.getProtocol().canKeepAlive())
			//	builder.setKeepAlive(true);
			
			KadMessage sent = builder.build();
			
			conn.sendMessage(sent);
			KadMessage recved = conn.recvMessage();
			
			/*
			if (sent.isKeepAlive() && recved.isKeepAlive()) {
				keepalive = true;
				proxyServer.register(conn);
			}
			*/
			
			opExecutor.createInsertNodeOperation(recved.getLastHop()).call();
			
		} finally {
			if (conn != null && keepalive == false)
				conn.close();
		}
		logger.info("node lookup for myself");
		opExecutor.createNodeLookupOperation(localNode.getKey(), kbuckets.getBucketSize()).call();
		
		logger.info("buckets refresh");
		for (int i=0; i < kbuckets.getNrBuckets(); ++i) {
			opExecutor.createBucketRefreshOperation(i).call();
		}
		
		return null;
	}

}
