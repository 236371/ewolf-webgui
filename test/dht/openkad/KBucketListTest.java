package dht.openkad;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import dht.Key;
import dht.openkad.ops.KadOperation;
import dht.openkad.ops.KadOperationsExecutor;

public class KBucketListTest {

	private static class KeyMatcher extends ArgumentMatcher<Key> {

		private Set<Key> keys = new HashSet<Key>();
		
		public KeyMatcher addKey(Key key) {
			keys.add(key);
			return this;
		}
		
		@Override
		public boolean matches(Object argument) {
			Assert.assertEquals(Key.class, argument.getClass());
			return keys.contains(argument);
		}
		
	}
	@Test(timeout=5000)
	public void init() throws Exception {
		KadNode mockedLocalNode = mock(KadNode.class);
		KadKeyFactory kf = new KadKeyFactory(0);
		KBucketsList kbucketsList = new KBucketsList(20, mockedLocalNode, kf);
		Assert.assertEquals(kf.getBitCount(), kbucketsList.getNrBuckets());
	}
	
	@Test(timeout=5000)
	public void insert() throws Exception {
		KadNode mockedLocalNode = mock(KadNode.class);
		KadKeyFactory kf = new KadKeyFactory(0);
		
		when(mockedLocalNode.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0010"+"1010", 2)));
		
		KBucketsList kbucketsList = new KBucketsList(20, mockedLocalNode, kf);
		
		KadNode mockedKadNode = mock(KadNode.class);
		when(mockedKadNode.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0010"+"1011", 2)));
		
		kbucketsList.insertNode(mockedKadNode);
		Assert.assertFalse(kbucketsList.getKBuckets()[0].getNodes().isEmpty());
		
		mockedKadNode = mock(KadNode.class);
		when(mockedKadNode.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1110"+"1000"+"0010"+"1011", 2)));
		
		kbucketsList.insertNode(mockedKadNode);
		Assert.assertFalse(kbucketsList.getKBuckets()[12].getNodes().isEmpty());
	}
	
	
	@Test(timeout=5000)
	public void closeNodes() throws Exception {
		KadNode mockedLocalNode = mock(KadNode.class);
		KadKeyFactory kf = new KadKeyFactory(0);
		
		when(mockedLocalNode.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0010"+"1010", 2)));
		
		KBucketsList kbucketsList = new KBucketsList(20, mockedLocalNode, kf);
		
		KadNode mockedKadNode1 = mock(KadNode.class);
		when(mockedKadNode1.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0010"+"0000", 2)));
		when(mockedKadNode1.toString()).thenReturn("mockedKadNode1");
		
		KadNode mockedKadNode2 = mock(KadNode.class);
		when(mockedKadNode2.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0000"+"0001", 2)));
		when(mockedKadNode2.toString()).thenReturn("mockedKadNode2");
		
		KadNode mockedKadNode3 = mock(KadNode.class);
		when(mockedKadNode3.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0011"+"0010", 2)));
		when(mockedKadNode3.toString()).thenReturn("mockedKadNode3");
		
		KadNode mockedKadNode4 = mock(KadNode.class);
		when(mockedKadNode4.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1011"+"1000"+"0010"+"0010", 2)));
		when(mockedKadNode4.toString()).thenReturn("mockedKadNode4");
		
		
		kbucketsList.insertNode(mockedKadNode1);
		kbucketsList.insertNode(mockedKadNode2);
		kbucketsList.insertNode(mockedKadNode3);
		kbucketsList.insertNode(mockedKadNode4);
		
		List<KadNode> kClosestNodes = kbucketsList.getKClosestNodes(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0010"+"1010", 2)), 3);
		
		Assert.assertEquals(3, kClosestNodes.size());
		Assert.assertTrue(kClosestNodes.contains(mockedKadNode1));
		Assert.assertTrue(kClosestNodes.contains(mockedKadNode2));
		Assert.assertTrue(kClosestNodes.contains(mockedKadNode3));
		Assert.assertFalse(kClosestNodes.contains(mockedKadNode4));
		
		Assert.assertTrue(kClosestNodes.get(0).equals(mockedKadNode1));
		Assert.assertTrue(kClosestNodes.get(1).equals(mockedKadNode3));
		Assert.assertTrue(kClosestNodes.get(2).equals(mockedKadNode2));
		
		//System.out.println(kClosestNodes);
	}
	
	@Test(timeout=5000)
	public void refreshBucket() throws Exception {
		KadNode mockedLocalNode = mock(KadNode.class);
		final KadKeyFactory kf = new KadKeyFactory();
		
		when(mockedLocalNode.getKey()).thenReturn(kf.getFromInt(new BigInteger(
				"1111"+"1000"+"0010"+"1010", 2)));
		
		KBucketsList kbucketsList = new KBucketsList(20, mockedLocalNode, kf);
		
		
		// check for bucket 2
		KeyMatcher keyMatcher = new KeyMatcher()
			//                                   "1111"+"1000"+"0010"+"1010"
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"1100", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"1101", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"1110", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"1111", 2)));
		
		for (int i=0; i < 8; ++i) {
			KadOperationsExecutor mockedOpExecutor = mock(KadOperationsExecutor.class);
			@SuppressWarnings("unchecked")
			KadOperation<List<KadNode>> op = mock(KadOperation.class);
			when(mockedOpExecutor.createNodeLookupOperation(any(Key.class))).thenReturn(op);
			
			kbucketsList.refreshBucket(2, mockedOpExecutor);
			verify(mockedOpExecutor).createNodeLookupOperation(argThat(keyMatcher));
		}
		
		//check for bucket 3
		keyMatcher = new KeyMatcher()
			//                                   "1111"+"1000"+"0010"+"1010"
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0000", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0001", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0010", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0011", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0100", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0101", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0110", 2)))
			.addKey(kf.getFromInt(new BigInteger("1111"+"1000"+"0010"+"0111", 2)));
	
		for (int i=0; i < 16; ++i) {
			KadOperationsExecutor mockedOpExecutor = mock(KadOperationsExecutor.class);
			@SuppressWarnings("unchecked")
			KadOperation<List<KadNode>> op = mock(KadOperation.class);
			when(mockedOpExecutor.createNodeLookupOperation(any(Key.class))).thenReturn(op);
			
			kbucketsList.refreshBucket(3, mockedOpExecutor);
			verify(mockedOpExecutor).createNodeLookupOperation(argThat(keyMatcher));
		}
	}
}
