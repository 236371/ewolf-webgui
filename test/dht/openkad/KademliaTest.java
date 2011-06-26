package dht.openkad;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import com.google.inject.Guice;
import com.google.inject.Injector;

import dht.Key;
import dht.SerializerFactory;
import dht.openkad.KadMsg.RPC;
import dht.openkad.validator.KadMsgValidator;

public class KademliaTest {
	
	private SerializerFactory serializer = new ObjectSerializerFactory();
	
	@Test(timeout=5000)
	public void create() throws Exception {
		KademliaTestModule mod = new KademliaTestModule();
		MockitoAnnotations.initMocks(mod);
		Injector injector = Guice.createInjector(mod);
		Kademlia kad = injector.getInstance(Kademlia.class);
		Assert.assertNotNull(kad);
		
		// adding an endpoint
		KadEndpoint mockedKadEndpoint = mock(KadEndpoint.class);
		mod.getEndpoints().add(mockedKadEndpoint);
		
		kad.create();
		verify(mod.getEndpoints()).iterator();
		ArgumentCaptor<KadConnectionListener> argument = ArgumentCaptor.forClass(KadConnectionListener.class);
		verify(mockedKadEndpoint).publish(argument.capture());
		Assert.assertEquals(mod.getConnDispacher(), argument.getValue());
		
	}
	
	@SuppressWarnings("unchecked")
	@Test(timeout=5000)
	public void join() throws Exception {
		KademliaTestModule mod = new KademliaTestModule();
		MockitoAnnotations.initMocks(mod);
		Injector injector = Guice.createInjector(mod);
		Kademlia kad = injector.getInstance(Kademlia.class);
		Assert.assertNotNull(kad);
		
		String key = mod.getKeyFactory().generate().toBase64();
		Future<Void> mockedFuture = mock(Future.class);
		when(mod.getOpExecutor().submitJoinOperation(any(KadNode.class))).thenReturn(mockedFuture);
		
		kad.join(new URI("tcpkad://1.2.3.4:5678/"+key));
		
		ArgumentCaptor<KadNode> nodeArg = ArgumentCaptor.forClass(KadNode.class);
		verify(mod.getOpExecutor(), times(1)).submitJoinOperation(nodeArg.capture());
		Assert.assertEquals(key, nodeArg.getValue().getKey().toBase64());
		Assert.assertEquals("1.2.3.4", nodeArg.getValue().getAddr().getHostAddress());
		
	}
	
	@Test(timeout=5000)
	public void incommingPing() throws Exception {
		KademliaTestModule mod = new KademliaTestModule();
		MockitoAnnotations.initMocks(mod);
		Injector injector = Guice.createInjector(mod);
		Kademlia kad = injector.getInstance(Kademlia.class);
		Assert.assertNotNull(kad);
		
		KadConnection mockedConn;
		KadNode remoteNode = mock(KadNode.class);
		KadNode someNode = mock(KadNode.class);
		Key remoteKey = mod.getKeyFactory().generate();
		Key localKey = mod.getKeyFactory().generate();
		Key someKey = mod.getKeyFactory().generate();
		when(mod.getLocalNode().getKey()).thenReturn(localKey);
		when(remoteNode.getKey()).thenReturn(remoteKey);
		when(someNode.getKey()).thenReturn(someKey);
		
		ArgumentCaptor<KadMsg> msgArg;
		
		KadMsg ping = new KadMsgBuilder()
			.setSrc(remoteNode)
			.setRpc(RPC.PING)
			.buildMessage();
		
		mockedConn = mock(KadConnection.class);
		when(mockedConn.recvMessage(any(KadMsgValidator.class))).thenReturn(ping);
		
		kad.onIncomingConnection(mockedConn);
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(RPC.PING, msgArg.getValue().getRpc());
		Assert.assertEquals(localKey, msgArg.getValue().getSrc().getKey());
		
	}
	
	@SuppressWarnings("unchecked")
	@Test(timeout=5000)
	public void incommingStore() throws Exception {
		KademliaTestModule mod = new KademliaTestModule();
		MockitoAnnotations.initMocks(mod);
		Injector injector = Guice.createInjector(mod);
		Kademlia kad = injector.getInstance(Kademlia.class);
		Assert.assertNotNull(kad);
		
		KadConnection mockedConn;
		KadNode remoteNode = mock(KadNode.class);
		KadNode someNode = mock(KadNode.class);
		Key remoteKey = mod.getKeyFactory().generate();
		Key localKey = mod.getKeyFactory().generate();
		Key someKey = mod.getKeyFactory().generate();
		String someValue = "someValue";
		when(mod.getLocalNode().getKey()).thenReturn(localKey);
		when(remoteNode.getKey()).thenReturn(remoteKey);
		when(someNode.getKey()).thenReturn(someKey);
		
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Collection> colArg;
		ArgumentCaptor<Key> keyArg;
		

		KadMsg store = new KadMsgBuilder()
			.setSrc(remoteNode)
			.setRpc(RPC.STORE)
			.setKey(someKey)
			.addValues(serializer, someValue)
			.buildMessage();
		
		mockedConn = mock(KadConnection.class);
		when(mockedConn.recvMessage(any(KadMsgValidator.class))).thenReturn(store);
		
		kad.onIncomingConnection(mockedConn);
		verify(mockedConn, never()).sendMessage(any(KadMsg.class));
		colArg = ArgumentCaptor.forClass(Collection.class);
		keyArg = ArgumentCaptor.forClass(Key.class);
		verify(mod.getLocalStorage(), times(1)).putAll(keyArg.capture(), colArg.capture());
		Assert.assertEquals(someKey, keyArg.getValue());
		
		Assert.assertEquals(1, colArg.getValue().size());
		Assert.assertEquals(someValue, serializer.createObjectInput(
				new ByteArrayInputStream((byte[])((List<byte[]>)colArg.getValue()).get(0)))
				.readObject());
	}
	
	@Test(timeout=5000)
	public void incommingFindNode() throws Exception {
		KademliaTestModule mod = new KademliaTestModule();
		MockitoAnnotations.initMocks(mod);
		Injector injector = Guice.createInjector(mod);
		Kademlia kad = injector.getInstance(Kademlia.class);
		Assert.assertNotNull(kad);
		
		KadConnection mockedConn;
		KadNode remoteNode = mock(KadNode.class);
		KadNode someNode1 = mock(KadNode.class);
		KadNode someNode2 = mock(KadNode.class);
		Key remoteKey = mod.getKeyFactory().generate();
		Key localKey = mod.getKeyFactory().generate();
		Key someKey1 = mod.getKeyFactory().generate();
		Key someKey2 = mod.getKeyFactory().generate();
		when(mod.getLocalNode().getKey()).thenReturn(localKey);
		when(remoteNode.getKey()).thenReturn(remoteKey);
		when(someNode1.getKey()).thenReturn(someKey1);
		when(someNode2.getKey()).thenReturn(someKey2);
		
		ArgumentCaptor<KadMsg> msgArg;
		ArgumentCaptor<Key> keyArg;
		
		
		KadMsg findNode = new KadMsgBuilder()
			.setSrc(remoteNode)
			.setRpc(RPC.FIND_NODE)
			.setKey(someKey1)
			.buildMessage();
		
		mockedConn = mock(KadConnection.class);
		when(mockedConn.recvMessage(any(KadMsgValidator.class))).thenReturn(findNode);
		when(mod.getKbuckets().getKClosestNodes(any(Key.class))).thenReturn(Arrays.asList(new KadNode[] {someNode1, someNode2}));
		
		kad.onIncomingConnection(mockedConn);
		
		keyArg = ArgumentCaptor.forClass(Key.class);
		verify(mod.getKbuckets(), times(1)).getKClosestNodes(keyArg.capture());
		Assert.assertEquals(someKey1, keyArg.getValue());
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(RPC.FIND_NODE, msgArg.getValue().getRpc());
		Assert.assertEquals(localKey, msgArg.getValue().getSrc().getKey());
		Assert.assertEquals(2, msgArg.getValue().getKnownClosestNodes().size());
		Assert.assertTrue(msgArg.getValue().getKnownClosestNodes().contains(someNode1));
		Assert.assertTrue(msgArg.getValue().getKnownClosestNodes().contains(someNode2));
	}
	
	@Test(timeout=5000)
	public void incommingFindValue() throws Exception {
		KademliaTestModule mod = new KademliaTestModule();
		MockitoAnnotations.initMocks(mod);
		Injector injector = Guice.createInjector(mod);
		Kademlia kad = injector.getInstance(Kademlia.class);
		Assert.assertNotNull(kad);
		
		KadConnection mockedConn;
		KadNode remoteNode = mock(KadNode.class);
		KadNode someNode = mock(KadNode.class);
		Key remoteKey = mod.getKeyFactory().generate();
		Key localKey = mod.getKeyFactory().generate();
		Key someKey = mod.getKeyFactory().generate();
		String someValue1 = "someValue1";
		String someValue2 = "someValue2";
		when(mod.getLocalNode().getKey()).thenReturn(localKey);
		when(remoteNode.getKey()).thenReturn(remoteKey);
		when(someNode.getKey()).thenReturn(someKey);
		
		ArgumentCaptor<KadMsg> msgArg;
		ArgumentCaptor<Key> keyArg;
		
		
		KadMsg findValue = new KadMsgBuilder()
			.setSrc(remoteNode)
			.setRpc(RPC.FIND_VALUE)
			.setKey(someKey)
			.buildMessage();
		
		Set<byte[]> vals = new HashSet<byte[]>();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutput objos = serializer.createObjectOutput(os);
		objos.writeObject(someValue1);
		objos.close();
		vals.add(os.toByteArray());
		
		os = new ByteArrayOutputStream();
		objos = serializer.createObjectOutput(os);
		objos.writeObject(someValue2);
		objos.close();
		vals.add(os.toByteArray());
		
		mockedConn = mock(KadConnection.class);
		when(mockedConn.recvMessage(any(KadMsgValidator.class))).thenReturn(findValue);
		when(mod.getKbuckets().getKClosestNodes(any(Key.class))).thenReturn(Arrays.asList(new KadNode[] {someNode}));
		when(mod.getLocalStorage().get(any(Key.class))).thenReturn(vals);
		kad.onIncomingConnection(mockedConn);
		
		keyArg = ArgumentCaptor.forClass(Key.class);
		verify(mod.getKbuckets(), times(1)).getKClosestNodes(keyArg.capture());
		Assert.assertEquals(someKey, keyArg.getValue());
		
		keyArg = ArgumentCaptor.forClass(Key.class);
		verify(mod.getLocalStorage(), times(1)).get(keyArg.capture());
		Assert.assertEquals(someKey, keyArg.getValue());
		
		msgArg = ArgumentCaptor.forClass(KadMsg.class);
		verify(mockedConn, times(1)).sendMessage(msgArg.capture());
		Assert.assertEquals(RPC.FIND_VALUE, msgArg.getValue().getRpc());
		Assert.assertEquals(localKey, msgArg.getValue().getSrc().getKey());
		Assert.assertTrue(msgArg.getValue().getKnownClosestNodes().contains(someNode));
		Assert.assertEquals(2, msgArg.getValue().getValues().size());
		Set<String> v = new HashSet<String>();
		for (byte[] b : msgArg.getValue().getValues()) {
			ByteArrayInputStream is = new ByteArrayInputStream(b);
			ObjectInput ois = serializer.createObjectInput(is);
			v.add((String)ois.readObject());
			ois.close();
		}
		Assert.assertTrue(v.contains(someValue1));
		Assert.assertTrue(v.contains(someValue2));
		
	}
}
