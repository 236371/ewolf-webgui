package il.technion.ewolf.kbr.openkad;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.DefaultNodeConnectionListener;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.Node;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;

public class KadNetTest {

	
	private final Collection<KeybasedRouting> kadnets = new ArrayList<KeybasedRouting>();
	
	@After
	public void teardown() throws Exception {
		for (KeybasedRouting kadnet : kadnets)
			kadnet.shutdown();
		Thread.sleep(5000);
	}
	
	@Test
	public void test2NodesTcp() throws Exception {
		
		Properties props = new Properties();
		props.setProperty("kadnet.otcpkad.port", "10001");
		props.setProperty("kadnet.bucketsize", "3");
		props.setProperty("kadnet.keyfactory.keysize", "2");
		KeybasedRouting kadnet1 = Guice
			.createInjector(new KadNetModule(props))
			.getInstance(KeybasedRouting.class);
		
		props.setProperty("kadnet.otcpkad.port", "10002");
		props.setProperty("kadnet.bucketsize", "3");
		props.setProperty("kadnet.keyfactory.keysize", "2");
		KeybasedRouting kadnet2 = Guice
			.createInjector(new KadNetModule(props))
			.getInstance(KeybasedRouting.class);
		
		kadnet1.create();
		kadnet2.create();
		kadnets.add(kadnet1);
		kadnets.add(kadnet2);
		
		Thread.sleep(1000);
		kadnet2.join(new URI("otcpkad://127.0.0.1:10001/")).get();
		List<Node> nodes;
		
		nodes = kadnet2.findNodes(kadnet1.getLocalNode().getKey(), 1).get();
		System.err.println(nodes);
		Assert.assertTrue(nodes.contains(kadnet1.getLocalNode()));
		
		nodes = kadnet1.findNodes(kadnet2.getLocalNode().getKey(), 1).get();
		Assert.assertTrue(nodes.contains(kadnet2.getLocalNode()));
	}
	
	
	@Test
	public void test2NodesUdp() throws Exception {
		
		Properties props = new Properties();
		props.setProperty("kadnet.oudpkad.port", "10001");
		props.setProperty("kadnet.bucketsize", "3");
		props.setProperty("kadnet.keyfactory.keysize", "2");
		KeybasedRouting kadnet1 = Guice
			.createInjector(new KadNetModule(props))
			.getInstance(KeybasedRouting.class);
		
		props.setProperty("kadnet.oudpkad.port", "10002");
		props.setProperty("kadnet.bucketsize", "3");
		props.setProperty("kadnet.keyfactory.keysize", "2");
		KeybasedRouting kadnet2 = Guice
			.createInjector(new KadNetModule(props))
			.getInstance(KeybasedRouting.class);
		
		kadnet1.create();
		kadnet2.create();
		kadnets.add(kadnet1);
		kadnets.add(kadnet2);
		
		Thread.sleep(1000);
		
		kadnet2.join(new URI("oudpkad://127.0.0.1:10001/")).get();
		List<Node> nodes;
		
		nodes = kadnet2.findNodes(kadnet1.getLocalNode().getKey(), 1).get();
		Assert.assertTrue(nodes.contains(kadnet1.getLocalNode()));
		
		nodes = kadnet1.findNodes(kadnet2.getLocalNode().getKey(), 1).get();
		Assert.assertTrue(nodes.contains(kadnet2.getLocalNode()));
	}
	
	@Test
	public void test16NodesTcp() throws Exception {
		Random rnd = new Random(9999);
		List<KeybasedRouting> nodes = new ArrayList<KeybasedRouting>();
		
		for (int i=0; i < 16; ++i) {
			Properties props = new Properties();
			props.setProperty("kadnet.keyfactory.seed", i+"");
			props.setProperty("kadnet.otcpkad.port", ""+(i+30000));
			props.setProperty("kadnet.bucketsize", "3");
			props.setProperty("kadnet.keyfactory.keysize", "2");
			KeybasedRouting kadnet = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
			kadnet.create();
			kadnets.add(kadnet);
			nodes.add(kadnet);
		}
		Thread.sleep(10000);
		
		for (int i=1; i < nodes.size(); ++i) {
			while (true) {
				int port = 30000+rnd.nextInt(i);
				try {
					nodes.get(i).join(new URI("otcpkad://127.0.0.1:"+port+"/")).get();
					break;
				} catch (Exception e) {
					System.err.println("Error connecting to "+port);
				}
			}
		}
		
		System.out.println("--------");
		KeybasedRouting kadnet1 = nodes.get(11);
		
		final KadKeyComparator comp = new KadKeyComparator(kadnet1.getLocalNode().getKey());
		List<KeybasedRouting> sortedNodes = new ArrayList<KeybasedRouting>(nodes);
		
		sortedNodes = sort(sortedNodes, on(KeybasedRouting.class).getLocalNode().getKey(), comp);
		
		/*
		Collections.sort(sortedNodes, new Comparator<KeybasedRouting>() {

			@Override
			public int compare(KeybasedRouting o1, KeybasedRouting o2) {
				return comp.compare(o1.getLocalNode(), o2.getLocalNode());
			}
		});
		*/
		/*
		for (int i=0; i < sortedNodes.size(); ++i) {
			System.out.println(sortedNodes.get(i));
		}
		*/
		
		List<Node> topNodes = new ArrayList<Node>();
		for (int i=0; i < 3; ++i) {
			topNodes.add(sortedNodes.get(i).getLocalNode());
		}
		
		for (int i=0; i < sortedNodes.size(); ++i) {
			List<Node> foundNodes = sortedNodes.get(i).findNodes(kadnet1.getLocalNode().getKey(), 3).get();
			Assert.assertEquals(""+i, topNodes, foundNodes);
		}
		/*
		KeybasedRouting kadnet2 = sortedNodes.get(15);
		System.err.println("Looking for: "+kadnet1.getLocalNode().getKey());
		System.err.println("Asking: "+kadnet2.getLocalNode().getKey());
		System.err.println("==============================");
		List<Node> foundNodes = kadnet2.findNodes(kadnet1.getLocalNode().getKey(), 3).get();
		System.err.println("==============================");
		System.out.println(foundNodes);
		*/
	}
	
	
	@Test
	public void test16NodesUdp() throws Exception {
		Random rnd = new Random(9999);
		List<KeybasedRouting> nodes = new ArrayList<KeybasedRouting>();
		
		for (int i=0; i < 16; ++i) {
			Properties props = new Properties();
			props.setProperty("kadnet.keyfactory.seed", i+"");
			props.setProperty("kadnet.oudpkad.port", ""+(i+30000));
			props.setProperty("kadnet.bucketsize", "3");
			props.setProperty("kadnet.keyfactory.keysize", "2");
			KeybasedRouting kadnet = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
			kadnet.create();
			nodes.add(kadnet);
			kadnets.add(kadnet);
		}
		Thread.sleep(10000);
		
		for (int i=1; i < nodes.size(); ++i) {
			while (true) {
				int port = 30000+rnd.nextInt(i);
				try {
					nodes.get(i).join(new URI("oudpkad://127.0.0.1:"+port+"/")).get();
					break;
				} catch (Exception e) {
					System.err.println("Error connecting to "+port);
				}
			}
		}
		
		
		KeybasedRouting kadnet1 = nodes.get(11);
		
		final KadKeyComparator comp = new KadKeyComparator(kadnet1.getLocalNode().getKey());
		List<KeybasedRouting> sortedNodes = new ArrayList<KeybasedRouting>(nodes);
		sortedNodes = sort(sortedNodes, on(KeybasedRouting.class).getLocalNode().getKey(), comp);
		/*
		Collections.sort(sortedNodes, new Comparator<KeybasedRouting>() {

			@Override
			public int compare(KeybasedRouting o1, KeybasedRouting o2) {
				return comp.compare(o1.getLocalNode(), o2.getLocalNode());
			}
		});
		*/
		/*
		for (int i=0; i < sortedNodes.size(); ++i) {
			System.out.println(sortedNodes.get(i));
		}
		*/
		
		List<Node> topNodes = new ArrayList<Node>();
		for (int i=0; i < 3; ++i) {
			topNodes.add(sortedNodes.get(i).getLocalNode());
		}
		
		for (int i=sortedNodes.size()-1; 0 <= i ; --i) {
			List<Node> foundNodes = sortedNodes.get(i).findNodes(kadnet1.getLocalNode().getKey(), 3).get();
			Assert.assertEquals(""+i, topNodes, foundNodes);
		}
		/*
		KeybasedRouting kadnet2 = sortedNodes.get(15);
		System.err.println("Looking for: "+kadnet1.getLocalNode().getKey());
		System.err.println("Asking: "+kadnet2.getLocalNode().getKey());
		System.err.println("==============================");
		List<Node> foundNodes = kadnet2.findNodes(kadnet1.getLocalNode().getKey(), 3).get();
		System.err.println("==============================");
		System.out.println(foundNodes);
		*/
	}
	
	@Test
	public void sendMessageTest() throws Exception {
		Random rnd = new Random(9999);
		List<KeybasedRouting> nodes = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 16; ++i) {
			Properties props = new Properties();
			props.setProperty("kadnet.keyfactory.seed", i+"");
			props.setProperty("kadnet.otcpkad.port", ""+(i+30000));
			props.setProperty("kadnet.bucketsize", "3");
			props.setProperty("kadnet.keyfactory.keysize", "2");
			KeybasedRouting kadnet = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
			kadnet.create();
			nodes.add(kadnet);
			kadnets.add(kadnet);
		}
		Thread.sleep(10000);
		
		for (int i=1; i < nodes.size(); ++i) {
			while (true) {
				int port = 30000+rnd.nextInt(i);
				try {
					nodes.get(i).join(new URI("otcpkad://127.0.0.1:"+port+"/")).get();
					break;
				} catch (Exception e) {
					System.err.println("Error connecting to "+port);
				}
			}
		}
		
		final byte[] bytes = new byte[4096];
		rnd.nextBytes(bytes);
		
		
		KeybasedRouting kadnet1 = nodes.get(11);
		
		KeybasedRouting kadnet2 = nodes.get(9);
		System.out.println("registering in "+kadnet2.getLocalNode());
		kadnet2.register("tag", new DefaultNodeConnectionListener() {
			
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in)
					throws IOException {
				Assert.assertEquals(bytes.length, in.available());
				byte[] b = new byte[bytes.length];
				in.read(b);
				Assert.assertArrayEquals(bytes, b);
				System.out.println("OK !!");
			}
			
			@Override
			public void onIncomingConnection(String tag, Node from, Socket conn) throws IOException {}
		});
		
		Node node = kadnet1.findNodes(kadnet2.getLocalNode().getKey(), 3).get().get(0);
		System.out.println("sending to "+node);
		OutputStream msg = node.sendMessage("tag");
		
		msg.write(bytes);
		msg.close();
	}
	
	
	
	@Test
	public void openConnectionTest1() throws Exception {
		Random rnd = new Random(9999);
		List<KeybasedRouting> nodes = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 16; ++i) {
			Properties props = new Properties();
			props.setProperty("kadnet.keyfactory.seed", i+"");
			props.setProperty("kadnet.otcpkad.port", ""+(i+30000));
			props.setProperty("kadnet.bucketsize", "3");
			props.setProperty("kadnet.keyfactory.keysize", "2");
			KeybasedRouting kadnet = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
			kadnet.create();
			nodes.add(kadnet);
			kadnets.add(kadnet);
		}
		Thread.sleep(10000);
		
		for (int i=1; i < nodes.size(); ++i) {
			while (true) {
				int port = 30000+rnd.nextInt(i);
				try {
					nodes.get(i).join(new URI("otcpkad://127.0.0.1:"+port+"/")).get();
					break;
				} catch (Exception e) {
					System.err.println("Error connecting to "+port);
				}
			}
		}
		
		final byte[] bytes = new byte[4096];
		rnd.nextBytes(bytes);
		
		
		KeybasedRouting kadnet1 = nodes.get(12);
		
		KeybasedRouting kadnet2 = nodes.get(8);
		System.out.println("registering in "+kadnet2.getLocalNode());
		kadnet2.register("tag", new DefaultNodeConnectionListener() {
			
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in) throws IOException {}
			
			@Override
			public void onIncomingConnection(String tag, Node from, Socket sock) throws IOException {
				byte[] b = new byte[3];
				sock.getInputStream().read(b);
				Assert.assertEquals("ABC", new String(b));
				
				sock.getOutputStream().write("EDF".getBytes());
				sock.getOutputStream().flush();
				sock.close();
			}
		});
		
		Node node = kadnet1.findNodes(kadnet2.getLocalNode().getKey(), 3).get().get(0);
		System.out.println("sending to "+node);
		Socket sock = node.openConnection("tag").get();
		
		sock.getOutputStream().write("ABC".getBytes());
		sock.getOutputStream().flush();
		byte[] b = new byte[3];
		sock.getInputStream().read(b);
		Assert.assertEquals("EDF", new String(b));
		
	}
	
	
	@Test
	public void openConnectionTest2() throws Exception {
		Random rnd = new Random(9999);
		List<KeybasedRouting> nodes = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 16; ++i) {
			Properties props = new Properties();
			props.setProperty("kadnet.keyfactory.seed", i+"");
			props.setProperty("kadnet.otcpkad.port", ""+(i+30000));
			props.setProperty("kadnet.bucketsize", "3");
			props.setProperty("kadnet.keyfactory.keysize", "2");
			if (i == 12)
				props.setProperty("kadnet.srv.conn.port", "-1");
			KeybasedRouting kadnet = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
			kadnet.create();
			nodes.add(kadnet);
			kadnets.add(kadnet);
		}
		Thread.sleep(10000);
		
		for (int i=1; i < nodes.size(); ++i) {
			while (true) {
				int port = 30000+rnd.nextInt(i);
				try {
					nodes.get(i).join(new URI("otcpkad://127.0.0.1:"+port+"/")).get();
					break;
				} catch (Exception e) {
					System.err.println("Error connecting to "+port);
				}
			}
		}
		
		final byte[] bytes = new byte[4096];
		rnd.nextBytes(bytes);
		
		
		KeybasedRouting kadnet1 = nodes.get(12);
		KeybasedRouting kadnet2 = nodes.get(8);
		System.out.println("registering in "+kadnet2.getLocalNode());
		kadnet2.register("tag", new DefaultNodeConnectionListener() {
			
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in) throws IOException {}
			
			@Override
			public void onIncomingConnection(String tag, Node from, Socket sock) throws IOException {
				byte[] b = new byte[3];
				sock.getInputStream().read(b);
				Assert.assertEquals("ABC", new String(b));
				
				sock.getOutputStream().write("EDF".getBytes());
				sock.getOutputStream().flush();
				sock.close();
			}
		});
		
		Node node = kadnet1.findNodes(kadnet2.getLocalNode().getKey(), 3).get().get(0);
		// node has no incoming connPort 
		System.out.println("sending to "+node);
		Socket sock = node.openConnection("tag").get();
		
		sock.getOutputStream().write("ABC".getBytes());
		sock.getOutputStream().flush();
		byte[] b = new byte[3];
		sock.getInputStream().read(b);
		Assert.assertEquals("EDF", new String(b));
		
	}
	
	
	
	@Test
	@Ignore
	public void test2NodesTcpAsymetric() throws Exception {
		
		Properties props = new Properties();
		props.setProperty("kadnet.otcpkad.port", "10001");
		props.setProperty("kadnet.bucketsize", "3");
		props.setProperty("kadnet.keyfactory.keysize", "2");
		KeybasedRouting kadnet1 = Guice
			.createInjector(new KadNetModule(props))
			.getInstance(KeybasedRouting.class);
		
		props.setProperty("kadnet.otcpkad.port", "-1");
		props.setProperty("kadnet.bucketsize", "3");
		props.setProperty("kadnet.keyfactory.keysize", "2");
		KeybasedRouting kadnet2 = Guice
			.createInjector(new KadNetModule(props))
			.getInstance(KeybasedRouting.class);
		
		kadnet1.create();
		kadnet2.create();
		kadnets.add(kadnet1);
		kadnets.add(kadnet2);
		
		Thread.sleep(1000);
		
		kadnet2.join(new URI("otcpkad://127.0.0.1:10001/")).get();
		List<Node> nodes;
		
		kadnet1.register("tag", new DefaultNodeConnectionListener() {
			
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in)
					throws IOException {
				System.out.println("IM HERE");
			}
			
			@Override
			public void onIncomingConnection(String tag, Node from, Socket sock)
					throws IOException {
				
			}
		});
		
		kadnet2.register("tag", new DefaultNodeConnectionListener() {
			
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in)
					throws IOException {
				System.out.println("IM HERE");
			}
			
			@Override
			public void onIncomingConnection(String tag, Node from, Socket sock)
					throws IOException {
				
			}
		});
		
		nodes = kadnet1.findNodes(kadnet2.getLocalNode().getKey(), 1).get();
		Assert.assertTrue(nodes.contains(kadnet2.getLocalNode()));
		
		Node node2FromNode1 = nodes.get(0);
		OutputStream msg = node2FromNode1.sendMessage("tag");
		msg.write("A".getBytes());
		msg.flush();
		
		nodes = kadnet2.findNodes(kadnet1.getLocalNode().getKey(), 1).get();
		Assert.assertTrue(nodes.contains(kadnet1.getLocalNode()));
		
		
	}
}
