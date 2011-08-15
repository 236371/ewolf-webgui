package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.OpenedKadConnections;

import java.net.Socket;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class KadOperationsExecutor {

	private final ExecutorService executor;
	private final KBuckets kbuckets;
	private final KadNode localNode;
	private final KeyFactory keyFactory;
	private final int concurrency;
	private final int connPort;
	//private final KadProxyServer proxyServer;
	private final OpenedKadConnections openedKadConnections;
	private final Logger logger;
	
	@Inject
	public KadOperationsExecutor(
			Logger logger,
			/*@Named("kadnet.logging.level.operations") */Level lvl,
			@Named("kadnet.executors.outgoing") ExecutorService executor,
			@Named("kadnet.concurrency") int concurrency,
			@Named("kadnet.srv.conn.port") int connPort,
			KBuckets kbuckets,
			@Named("kadnet.localnode") KadNode localNode,
			KeyFactory keyFactory,
			//KadProxyServer proxyServer,
			OpenedKadConnections openedKadConnections) {
		
		this.logger = logger;
		this.logger.setLevel(lvl);
		this.executor = executor;
		this.concurrency = concurrency;
		this.kbuckets = kbuckets;
		this.localNode = localNode;
		this.keyFactory = keyFactory;
		this.connPort = connPort;
		//this.proxyServer = proxyServer;
		this.openedKadConnections = openedKadConnections;
	}
	
	public KadNode getLocalNode() {
		return localNode;
	}
	
	public <T> Future<T> submit(Callable<T> op) {
		return executor.submit(op);
	}
	

	public KadOperation<List<KadNode>> createNodeLookupOperation(Key key, int n) {
		return new NodeLookupOperation(logger, localNode, openedKadConnections, kbuckets, this, concurrency, key, n);
	}
	public Future<List<KadNode>> submitNodeLookupOperation(Key key, int n) {
		return executor.submit(createNodeLookupOperation(key, n));
	}
	public void executeNodeLookupOperation(Key key, int n) {
		final KadOperation<List<KadNode>> op = createNodeLookupOperation(key, n);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) {}
			}
		});
	}
	
	
	public KadOperation<Void> createJoinOperation(URI bootstrap) {
		return new JoinOperation(logger, localNode, kbuckets, this, bootstrap/*, proxyServer*/);
	}
	public Future<Void> submitJoinOperation(URI bootstrap) {
		return executor.submit(createJoinOperation(bootstrap));
	}
	public void executeJoinOperation(URI bootstrap) {
		final KadOperation<Void> op = createJoinOperation(bootstrap);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) {}
			}
		});
	}
	
	
	public KadOperation<Void> createBucketRefreshOperation(int bucketNum) {
		return new BucketRefreshOperation(logger, localNode, this, keyFactory, kbuckets.getBucketSize(), bucketNum);
	}
	public Future<Void> submitBucketRefreshOperation(int bucketNum) {
		return executor.submit(createBucketRefreshOperation(bucketNum));
	}
	public void executeBucketRefreshOperation(int bucketNum) {
		final KadOperation<Void> op = createBucketRefreshOperation(bucketNum);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) {}
			}
		});
	}
	
	public KadOperation<Void> createInsertNodeOperation(KadNode ... nodes) {
		return new InsertNodeOperation(logger, kbuckets, nodes);
	}
	public Future<Void> submitInsertNodeOperation(KadNode ... nodes) {
		return executor.submit(createInsertNodeOperation(nodes));
	}
	public void executeInsertNodeOperation(KadNode ... nodes) {
		final KadOperation<Void> op = createInsertNodeOperation(nodes);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) {}
			}
		});
	}
	
	public KadOperation<Void> createInsertNodeIfNotFullOperation(Collection<KadNode> nodes) {
		return new InsertNodeIfNotFullOperation(logger, kbuckets, nodes);
	}
	public Future<Void> submitInsertNodeIfNotFullOperation(Collection<KadNode> nodes) {
		return executor.submit(createInsertNodeIfNotFullOperation(nodes));
	}
	public void executeInsertNodeIfNotFullOperation(Collection<KadNode> nodes) {
		final KadOperation<Void> op = createInsertNodeIfNotFullOperation(nodes);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) {}
			}
		});
	}
	
	
	public KadOperation<Socket> createOpenConnectionOperation(KadNode with, String tag) {
		return new OpenConnectionOperation(logger, localNode, connPort, with, tag);
	}
	public Future<Socket> submitOpenConnectionOperation(KadNode with, String tag) {
		return executor.submit(createOpenConnectionOperation(with, tag));
	}
}
