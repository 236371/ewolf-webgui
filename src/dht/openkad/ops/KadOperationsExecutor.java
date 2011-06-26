package dht.openkad.ops;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import dht.Key;
import dht.KeyFactory;
import dht.SerializerFactory;
import dht.openkad.KBucketsList;
import dht.openkad.KadNode;
import dht.openkad.validator.KadBasicMsgValidator;

public class KadOperationsExecutor {

	private final KadNode localNode;
	private final KBucketsList kbuckets;
	private final ExecutorService executor;
	private final int concurrencyFactor;
	private final KadBasicMsgValidator basicValidator;
	
	@Inject
	KadOperationsExecutor(
			@Named("kad.localnode") KadNode localNode,
			KBucketsList kbuckets,
			@Named("kad.concurrency") int concurrencyFactor,
			@Named("kad.operations.threadpool") ExecutorService executor,
			KeyFactory keyFactory) {
		
		this.localNode = localNode;
		this.kbuckets = kbuckets;
		this.executor = executor;
		this.concurrencyFactor = concurrencyFactor;
		this.basicValidator = new KadBasicMsgValidator(keyFactory);
	}
	
	
	// JoinOperation
	public KadOperation<Void> createJoinOperation(KadNode bootstrapNode) {
		return new JoinOperation(localNode, kbuckets, this, bootstrapNode);
	}
	public Future<Void> submitJoinOperation(KadNode bootstrapNode) {
		return executor.submit(createJoinOperation(bootstrapNode));
	}
	public void executeJoinOperation(KadNode bootstrapNode) {
		final KadOperation<Void> op = createJoinOperation(bootstrapNode);
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) { }
			}
		});
	}
	
	
	// NodeLookupOperation
	public KadOperation<List<KadNode>> createNodeLookupOperation(Key k) {
		return new NodeLookupOperation(localNode, kbuckets, concurrencyFactor, this, k, basicValidator);
	}
	public Future<List<KadNode>> submitNodeLookup(Key k) {
		return executor.submit(createNodeLookupOperation(k));
	}
	public void executeNodeLookup(Key k) {
		final KadOperation<List<KadNode>> op = createNodeLookupOperation(k);
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) { }
			}
		});
	}
	
	
	// InsertNodeIfNotFullOperation
	public KadOperation<Void> createInsertNodeIfNotFullOperation(KadNode ... nodesToInsert) {
		return new InsertNodeIfNotFullOperation(localNode, kbuckets, Arrays.asList(nodesToInsert));
	}
	public Future<Void> submitInsertNodeIfNotFull(KadNode ... nodesToInsert) {
		return executor.submit(createInsertNodeIfNotFullOperation(nodesToInsert));
	}
	public void executeInsertNodeIfNotFull(KadNode ... nodesToInsert) {
		final KadOperation<Void> op = createInsertNodeIfNotFullOperation(nodesToInsert);
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) { }
			}
		});
	}
	
	
	// InsertNodeOperation
	public KadOperation<Void> createInsertNodeOperation(KadNode ... nodesToInsert) {
		return new InsertNodeOperation(localNode, kbuckets, Arrays.asList(nodesToInsert));
	}
	public Future<Void> submitInsertNode(KadNode ... nodesToInsert) {
		return executor.submit(createInsertNodeOperation(nodesToInsert));
	}
	public void executeInsertNode(KadNode ... nodesToInsert) {
		final KadOperation<Void> op = createInsertNodeOperation(nodesToInsert);
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) { }
			}
		});
	}
	

	// RefreshBucketOperation
	public KadOperation<Void> createRefreshBucket(int bucketIndex) {
		return new KBucketRefreshOperation(localNode, kbuckets, this, bucketIndex);
	}
	public Future<Void> submitRefreshBucket(int bucketIndex) {
		return executor.submit(createRefreshBucket(bucketIndex));
	}
	public void executeRefreshBucket(int bucketIndex) {
		final KadOperation<Void> op = createRefreshBucket(bucketIndex);
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) { }
			}
		});
	}
	
	
	// StoreValueOperation
	public KadOperation<Void> createStoreValue(Key key, Object value, SerializerFactory serializer) throws IOException {
		return new StoreValueOperation(localNode, kbuckets, this, key, value, serializer);
	}
	public Future<Void> submitStoreValue(Key key, Object value, SerializerFactory serializer) throws IOException {
		return executor.submit(createStoreValue(key, value, serializer));
	}
	public void executeStoreValue(Key key, Serializable value, SerializerFactory serializer) throws IOException {
		final KadOperation<Void> op = createStoreValue(key, value, serializer);
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try { op.call(); } catch (Exception e) { }
			}
		});
	}
	
	
	// FindValuesOperation
	public KadOperation<Set<Object>> createFindValues(Key key, SerializerFactory serializer) throws IOException {
		return new FindValuesOperation(localNode, kbuckets, this, key, serializer, basicValidator);
	}
	public Future<Set<Object>> submitFindValues(Key key, SerializerFactory serializer) throws IOException {
		return executor.submit(createFindValues(key, serializer));
	}


	
}
