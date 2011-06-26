package dht.openkad.ops;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import dht.Key;
import dht.SerializerFactory;
import dht.openkad.KBucketsList;
import dht.openkad.KadConnection;
import dht.openkad.KadKeyFactory;
import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;
import dht.openkad.KadMsgBuilder;
import dht.openkad.KadNode;
import dht.openkad.KadNodeComparator;
import dht.openkad.validator.KadBasicMsgValidator;
import dht.openkad.validator.KadMsgValidator;

public class OperationsTest {

	@Mock private KadOperationsExecutor opExecutor;
	@Mock private KBucketsList kbuckets;
	private SerializerFactory serializer = new SerializerFactory() {

		@Override
		public ObjectInput createObjectInput(InputStream is) throws IOException {
			return new ObjectInputStream(is);
		}

		@Override
		public ObjectOutput createObjectOutput(OutputStream os)
				throws IOException {
			return new ObjectOutputStream(os);
		}
	};
	
	
	//@Mock private Future<Void> mockedFuture;
	private KadKeyFactory keyFactory = new KadKeyFactory();
	
	public OperationsTest() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test(timeout=5000)
	public void insertNodeOperationTest() throws Exception {
		
		KadNode localNode = mock(KadNode.class);
		when(localNode.getKey()).thenReturn(keyFactory.generate());
		Collection<KadNode> nodesToInsert = new ArrayList<KadNode>();
		
		KadNode n1 = mock(KadNode.class);
		when(n1.getKey()).thenReturn(keyFactory.generate());
		nodesToInsert.add(n1);
		
		KadNode n2 = mock(KadNode.class);
		when(n2.getKey()).thenReturn(keyFactory.generate());
		nodesToInsert.add(n2);
		
		KadNode n3 = mock(KadNode.class);
		when(n3.getKey()).thenReturn(keyFactory.generate());
		nodesToInsert.add(n3);
		
		
		InsertNodeOperation op = new InsertNodeOperation(localNode, kbuckets, nodesToInsert);
		
		op.call();
		
		ArgumentCaptor<KadNode> nodeArg = ArgumentCaptor.forClass(KadNode.class);
		verify(kbuckets, times(3)).insertNode(nodeArg.capture());
		Assert.assertTrue(nodeArg.getAllValues().containsAll(nodesToInsert));
		
	}
	
	@Test(timeout=5000)
	public void joinOperationTest() throws Exception {
		
		// setup local key
		Key localKey = keyFactory.generate();
		KadNode localNode = mock(KadNode.class);
		when(localNode.getKey()).thenReturn(localKey);
		
		// setup op executor
		InsertNodeOperation mockedInsertNodeOp = mock(InsertNodeOperation.class);
		NodeLookupOperation mockedNodeLookupOp = mock(NodeLookupOperation.class);
		
		when(opExecutor.createInsertNodeOperation(any(KadNode.class))).thenReturn(mockedInsertNodeOp);
		when(opExecutor.createNodeLookupOperation(any(Key.class))).thenReturn(mockedNodeLookupOp);
		@SuppressWarnings("unchecked")
		KadOperation<Void> mockedVoidOp = mock(KadOperation.class);
		when(opExecutor.createRefreshBucket(anyInt())).thenReturn(mockedVoidOp);
		// setup bootstrap node
		KadNode bootstrapNode = mock(KadNode.class);
		Key bootstrapKey = keyFactory.generate();
		when(bootstrapNode.getKey()).thenReturn(bootstrapKey);
		
		// setup kbuckets list
		when(kbuckets.getNrBuckets()).thenReturn(160);
		
		// calling JoinOperation !!!
		JoinOperation op = new JoinOperation(localNode, kbuckets, opExecutor, bootstrapNode);
		op.call();
		
		// checking
		ArgumentCaptor<KadNode> nodeArg = ArgumentCaptor.forClass(KadNode.class);
		verify(opExecutor, times(1)).createInsertNodeOperation(nodeArg.capture());
		Assert.assertEquals(bootstrapNode.getKey(), nodeArg.getValue().getKey());
		
		ArgumentCaptor<Key> keyArg = ArgumentCaptor.forClass(Key.class);
		verify(opExecutor, times(1)).createNodeLookupOperation(keyArg.capture());
		Assert.assertEquals(localKey, keyArg.getValue());
		
		verify(opExecutor, times(160)).createRefreshBucket(anyInt());
	}
	
	@Test(timeout=5000)
	public void findValuesOperationTest() throws Exception {
		// setup local key
		Key localKey = keyFactory.generate();
		KadNode localNode = mock(KadNode.class);
		when(localNode.getKey()).thenReturn(localKey);
		
		// setup some random key
		Key someKey = keyFactory.generate();
		
		// setup op executor
		List<KadNode> kClosestNodes = new ArrayList<KadNode>();
		
		KadNode n1 = mock(KadNode.class);
		when(n1.getKey()).thenReturn(keyFactory.generate());
		KadConnection mockedConn1 = mock(KadConnection.class);
		when(n1.openConnection()).thenReturn(mockedConn1);
		when(mockedConn1.recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class))).thenReturn(new KadMsgBuilder()
			.setKey(someKey)
			.setSrc(n1)
			.setRpc(RPC.FIND_VALUE)
			.addValues(serializer, "v_a1", "v_a2")
			.buildMessage());
		kClosestNodes.add(n1);
		
		KadNode n2 = mock(KadNode.class);
		when(n2.getKey()).thenReturn(keyFactory.generate());
		KadConnection mockedConn2 = mock(KadConnection.class);
		when(n2.openConnection()).thenReturn(mockedConn2);
		when(mockedConn2.recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class))).thenThrow(new IOException());
		kClosestNodes.add(n2);
		
		KadNode n3 = mock(KadNode.class);
		when(n3.getKey()).thenReturn(keyFactory.generate());
		KadConnection mockedConn3 = mock(KadConnection.class);
		when(n3.openConnection()).thenReturn(mockedConn3);
		when(mockedConn3.recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class))).thenReturn(new KadMsgBuilder()
			.setKey(someKey)
			.setSrc(n3)
			.setRpc(RPC.FIND_VALUE)
			.addValues(serializer, "v_c1", "v_c2")
			.buildMessage());
		kClosestNodes.add(n3);
		
		NodeLookupOperation mockedNodeLookupOp = mock(NodeLookupOperation.class);
		when(mockedNodeLookupOp.call()).thenReturn(kClosestNodes);
		when(opExecutor.createNodeLookupOperation(any(Key.class))).thenReturn(mockedNodeLookupOp);
		
		
		// calling FindValuesOperation !!
		FindValuesOperation op = new FindValuesOperation(localNode, kbuckets, opExecutor, someKey, serializer, new KadBasicMsgValidator(keyFactory));
		Set<Object> ret = op.call();
		
		// checking node lookup was performed
		ArgumentCaptor<Key> keyArg = ArgumentCaptor.forClass(Key.class);
		verify(opExecutor, times(1)).createNodeLookupOperation(keyArg.capture());
		Assert.assertEquals(someKey, keyArg.getValue());
		
		// check FIND_VALUE was send to all nodes
		verify(n1, times(1)).openConnection();
		verify(n2, times(1)).openConnection();
		verify(n3, times(1)).openConnection();
		
		ArgumentCaptor<KadMsg> msgArg;
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn1, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(someKey, msgArg.getValue().getKey());
		Assert.assertEquals(RPC.FIND_VALUE, msgArg.getValue().getRpc());
		Assert.assertEquals(localNode, msgArg.getValue().getSrc());
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn2, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(someKey, msgArg.getValue().getKey());
		Assert.assertEquals(RPC.FIND_VALUE, msgArg.getValue().getRpc());
		Assert.assertEquals(localNode, msgArg.getValue().getSrc());
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn3, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(someKey, msgArg.getValue().getKey());
		Assert.assertEquals(RPC.FIND_VALUE, msgArg.getValue().getRpc());
		Assert.assertEquals(localNode, msgArg.getValue().getSrc());
		
		
		// check FIND_VALUE was recved
		verify(mockedConn1, times(1)).recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class));
		verify(mockedConn2, times(1)).recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class));
		verify(mockedConn3, times(1)).recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class));
		
		Assert.assertTrue(ret.containsAll(Arrays.asList(new String[] {"v_a1", "v_a2", "v_c1", "v_c2"})));
	}
	
	@Test(timeout=5000)
	public void storeValueOperationTest() throws Exception {
		// setup local key
		Key localKey = keyFactory.generate();
		KadNode localNode = mock(KadNode.class);
		when(localNode.getKey()).thenReturn(localKey);
		
		// setup some random key
		Key someKey = keyFactory.generate();
		String someValue = "some value";
		
		List<KadNode> kClosestNodes = new ArrayList<KadNode>();
		
		KadNode n1 = mock(KadNode.class);
		when(n1.getKey()).thenReturn(keyFactory.generate());
		KadConnection mockedConn1 = mock(KadConnection.class);
		when(n1.openConnection()).thenReturn(mockedConn1);
		kClosestNodes.add(n1);
		
		KadNode n2 = mock(KadNode.class);
		when(n2.getKey()).thenReturn(keyFactory.generate());
		when(n2.openConnection()).thenThrow(new IOException());
		kClosestNodes.add(n2);
		
		KadNode n3 = mock(KadNode.class);
		when(n3.getKey()).thenReturn(keyFactory.generate());
		KadConnection mockedConn3 = mock(KadConnection.class);
		when(n3.openConnection()).thenReturn(mockedConn3);
		kClosestNodes.add(n3);
		
		NodeLookupOperation mockedNodeLookupOp = mock(NodeLookupOperation.class);
		when(mockedNodeLookupOp.call()).thenReturn(kClosestNodes);
		when(opExecutor.createNodeLookupOperation(any(Key.class))).thenReturn(mockedNodeLookupOp);
		
		// calling StoreValueOperation
		StoreValueOperation op = new StoreValueOperation(localNode, kbuckets, opExecutor, someKey, someValue, serializer);
		op.call();
		
		// checking node lookup operation was called
		ArgumentCaptor<Key> keyArg = ArgumentCaptor.forClass(Key.class);
		verify(opExecutor, times(1)).createNodeLookupOperation(keyArg.capture());
		Assert.assertEquals(someKey, keyArg.getValue());
		
		// check all nodes were send msgs
		ArgumentCaptor<KadMsg> msgArg;
		
		verify(n1, times(1)).openConnection();
		verify(n2, times(1)).openConnection();
		verify(n3, times(1)).openConnection();
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn1, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(RPC.STORE, msgArg.getValue().getRpc());
		Assert.assertEquals(1, msgArg.getValue().getValues().size());
		Assert.assertEquals(someValue, serializer.createObjectInput(new ByteArrayInputStream(msgArg.getValue().getValues().get(0))).readObject());
		Assert.assertEquals(someKey, msgArg.getValue().getKey());
		Assert.assertEquals(localNode, msgArg.getValue().getSrc());
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn3, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(RPC.STORE, msgArg.getValue().getRpc());
		Assert.assertEquals(1, msgArg.getValue().getValues().size());
		Assert.assertEquals(someValue, serializer.createObjectInput(new ByteArrayInputStream(msgArg.getValue().getValues().get(0))).readObject());
		Assert.assertEquals(someKey, msgArg.getValue().getKey());
		Assert.assertEquals(localNode, msgArg.getValue().getSrc());
	}
	
	@Test(timeout=5000)
	public void kBucketRefreshOperationTest() throws Exception {
		// setup local key
		Key localKey = keyFactory.generate();
		KadNode localNode = mock(KadNode.class);
		when(localNode.getKey()).thenReturn(localKey);
		
		// call KBucketRefreshOperation
		KBucketRefreshOperation op = new KBucketRefreshOperation(localNode, kbuckets, opExecutor, 10);
		op.call();
		
		ArgumentCaptor<Integer> intArg = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<KadOperationsExecutor> opExecutorArg = ArgumentCaptor.forClass(KadOperationsExecutor.class);
		verify(kbuckets, times(1)).refreshBucket(intArg.capture(), opExecutorArg.capture());
		Assert.assertEquals(new Integer(10), intArg.getValue());
		Assert.assertEquals(opExecutor, opExecutorArg.getValue());
		
	}
	
	@Test(timeout=5000)
	public void nodeLookupOperationTest() throws Exception {
		// setup local key
		Key localKey = keyFactory.generate();
		KadNode localNode = mock(KadNode.class);
		when(localNode.getKey()).thenReturn(localKey);
		
		// setup some random key
		Key someKey = keyFactory.generate();
		
		// setup nodes
		List<KadNode> kClosestNodes = new ArrayList<KadNode>();
		List<KadConnection> mockedConn = new ArrayList<KadConnection>();
		for (int i=0; i < 20; ++i) {
			KadNode n = mock(KadNode.class);
			when(n.getKey()).thenReturn(keyFactory.generate());
			KadConnection conn = mock(KadConnection.class);
			mockedConn.add(conn);
			when(n.openConnection()).thenReturn(conn);
			kClosestNodes.add(n);
		}
		Collections.sort(kClosestNodes, new KadNodeComparator(someKey));
		List<KadNode> l1 = new ArrayList<KadNode>(kClosestNodes.subList(0, 10));
		List<KadNode> l2 = new ArrayList<KadNode>(kClosestNodes.subList(10, 20));
		for (KadNode n : kClosestNodes) {
			when(n.openConnection().recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class))).thenReturn(new KadMsgBuilder()
				.setSrc(n)
				.setKey(someKey)
				.setRpc(RPC.FIND_NODE)
				.addCloseNodes(l2)
				.buildMessage());
		}
		
		when(kbuckets.getKClosestNodes(any(Key.class))).thenReturn(l1);
		when(kbuckets.getBucketSize()).thenReturn(20);
		// call NodeLookupOperation
		NodeLookupOperation op = new NodeLookupOperation(localNode, kbuckets, 2, opExecutor, someKey, new KadBasicMsgValidator(keyFactory));
		List<KadNode> ret = op.call();
		
		// checking
		ArgumentCaptor<Key> keyArg = ArgumentCaptor.forClass(Key.class);
		verify(kbuckets, times(1)).getKClosestNodes(keyArg.capture());
		Assert.assertEquals(someKey, keyArg.getValue());
		
		Assert.assertEquals(kClosestNodes, ret);
		
		for (KadConnection conn : mockedConn) {
			verify(conn, times(1)).recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class));
			verify(conn, times(1)).sendMessage(any(KadMsg.class));
		}
	}
}
