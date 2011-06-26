package dht.openkad;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import dht.KeyFactory;
import dht.openkad.KadMsg.RPC;
import dht.openkad.validator.KadBasicMsgValidator;
import dht.openkad.validator.KadMsgValidator;
public class KBucketTest {

	@Test(timeout=5000)
	public void init() throws Exception {
		KadNode mockedKadNode = mock(KadNode.class);
		KadBasicMsgValidator validator = new KadBasicMsgValidator(new KadKeyFactory());
		KBucket kbucket = new KBucket(20, mockedKadNode, validator);
		Assert.assertTrue(kbucket.getNodes().isEmpty());
	}
	
	@Test(timeout=5000)
	public void insert() throws Exception {
		KadNode mockedLocalNode = mock(KadNode.class);
		KeyFactory kf = new KadKeyFactory(0);
		KadBasicMsgValidator validator = new KadBasicMsgValidator(kf);
		KBucket kbucket = new KBucket(20, mockedLocalNode, validator);

		// one insert
		KadNode mockedKadNode1 = mock(KadNode.class);
		when(mockedKadNode1.getKey()).thenReturn(kf.generate());
		kbucket.insertNode(mockedKadNode1);
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode1));
		
		// double insert of the same node
		kbucket.insertNode(mockedKadNode1);
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode1));
		Assert.assertEquals(1, kbucket.getNodes().size());
		
		// insert another node
		KadNode mockedKadNode2 = mock(KadNode.class);
		when(mockedKadNode2.getKey()).thenReturn(kf.generate());
		kbucket.insertNode(mockedKadNode2);
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode1));
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode2));
		Assert.assertEquals(2, kbucket.getNodes().size());
		
	}
	@Test(timeout=5000)
	public void evacuation() throws Exception {
		KadNode mockedLocalNode = mock(KadNode.class);
		KeyFactory kf = new KadKeyFactory(0);
		KadBasicMsgValidator validator = new KadBasicMsgValidator(kf);
		KBucket kbucket = new KBucket(2, mockedLocalNode, validator);
		
		KadNode mockedKadNode1 = mock(KadNode.class);
		KadNode mockedKadNode2 = mock(KadNode.class);
		KadNode mockedKadNode3 = mock(KadNode.class);
		
		when(mockedKadNode1.getKey()).thenReturn(kf.generate());
		when(mockedKadNode2.getKey()).thenReturn(kf.generate());
		when(mockedKadNode3.getKey()).thenReturn(kf.generate());
		
		// insert the first 2
		kbucket.insertNode(mockedKadNode1);
		kbucket.insertNode(mockedKadNode2);
		
		Assert.assertEquals(2, kbucket.getNodes().size());
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode1));
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode2));
		// bucket: n1, n2
		
		// insert the 3rd and check PING
		kbucket.insertNode(mockedKadNode3);
		Assert.assertEquals(2, kbucket.getNodes().size());
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode2));
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode3));
		verify(mockedKadNode1).openConnection();
		// bucket: n2, n3
		
		// check PING msg was sent
		KadConnection mockedKadConn = mock(KadConnection.class);
		when(mockedKadNode2.openConnection()).thenReturn(mockedKadConn);
		when(mockedKadConn.recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class))).thenThrow(new IOException());
		kbucket.insertNode(mockedKadNode1);
		verify(mockedKadConn).sendMessage(any(KadMsg.class));
		Assert.assertEquals(2, kbucket.getNodes().size());
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode3));
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode1));
		// bucket: n3, n1
		
		mockedKadConn = mock(KadConnection.class);
		when(mockedKadNode3.openConnection()).thenReturn(mockedKadConn);
		when(mockedKadConn.recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class))).thenReturn(new KadMsgBuilder()
				.setRpc(RPC.PING)
				.setSrc(mockedKadNode3)
				.buildMessage());
		kbucket.insertNode(mockedKadNode2);
		Assert.assertEquals(2, kbucket.getNodes().size());
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode3));
		Assert.assertTrue(kbucket.getNodes().contains(mockedKadNode1));
		// bucket: n1, n3
		verify(mockedKadConn).sendMessage(any(KadMsg.class));
		verify(mockedKadConn).recvMessage(any(KadMsgValidator.class), any(KadMsgValidator.class));
	}
}
