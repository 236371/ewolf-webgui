package il.technion.ewolf.kbr;

import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class KeybasedRoutingTest {

	@Test
	public void the2NodesShouldFindEachOther() throws Throwable {
		int basePort = 10000;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 2; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "1")
					.setProperty("openkad.bucket.kbuckets.maxsize", "3")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		kbrs.get(1).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+basePort+"/")));
		System.out.println("finished joining");
		
		for (int i=0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}
		List<Node> findNode = kbrs.get(1).findNode(kbrs.get(0).getLocalNode().getKey(), 5);
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(1));
		
		findNode = kbrs.get(0).findNode(kbrs.get(0).getLocalNode().getKey(), 5);
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(1));
		
		findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey(), 5);
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(1));
		
		findNode = kbrs.get(1).findNode(kbrs.get(1).getLocalNode().getKey(), 5);
		Assert.assertEquals(kbrs.get(1).getLocalNode(), findNode.get(0));
		Assert.assertEquals(kbrs.get(0).getLocalNode(), findNode.get(1));
		
		System.out.println(findNode);
		
	}
	
	
	@Test
	public void the16NodesShouldFindEachOther() throws Throwable {
		int basePort = 10100;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 16; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "2")
					.setProperty("openkad.bucket.kbuckets.maxsize", "5")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
			
		System.out.println("finished joining");
		
		
		for (int i=0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}
		for (int j=0; j < kbrs.size(); ++j) {
			Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i=0; i < kbrs.size(); ++i) {
				List<Node> findNode = kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey(), 7);
				System.out.println(findNode);
				findNodeResults.add(findNode);
			}
			
			if (findNodeResults.size() != 1) {
				for (List<Node> n : findNodeResults)
					System.err.println(n);
			}
			Assert.assertEquals(1, findNodeResults.size());
		}
	}
	
	/*
	@Test
	public void the64NodesShouldFindEachOtherAsynchronously() throws Throwable {
		int basePort = 10800;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 64; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "2")
					.setProperty("openkad.bucket.kbuckets.maxsize", "5")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
			
		System.out.println("finished joining");
		
		
		for (int i=0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}
		
		List<Future<List<Node>>> futures = new ArrayList<Future<List<Node>>>();
		
		for (int j=0; j < kbrs.size(); ++j) {
			for (int i=0; i < kbrs.size(); ++i) {
				futures.add(kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey(), 5));
				//System.out.println(findNode);
				//findNodeResults.add(findNode);
			}
		}
		int i=0;
		for (Future<List<Node>> f : futures) {
			System.out.println(i++);
			f.get();
		}
	}
	*/
	
	@Test
	public void the64NodesShouldFindEachOther() throws Throwable {
		int basePort = 10200;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		Random rnd = new Random(10200);
		for (int i=0; i < 64; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "7")
					.setProperty("openkad.bucket.kbuckets.maxsize", "7")
					.setProperty("openkad.bucket.colors.nrcolors", "1")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + rnd.nextInt(i);
			System.out.println(i+" ==> "+(port-basePort));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
			
		System.out.println("finished joining");
		
		
		for (int i=0; i < kbrs.size(); ++i) {
			System.out.println(kbrs.get(i));
			System.out.println("======");
		}
		for (int j=0; j < kbrs.size(); ++j) {
			Set<List<Node>> findNodeResults = new HashSet<List<Node>>();
			for (int i=0; i < kbrs.size(); ++i) {
				List<Node> findNode = kbrs.get(i).findNode(kbrs.get(j).getLocalNode().getKey(), 7);
				System.out.println(findNode);
				findNodeResults.add(findNode);
			}
			
			if (findNodeResults.size() != 1) {
				for (List<Node> r : findNodeResults)
					System.err.println(r);
			}
			Assert.assertEquals(1, findNodeResults.size());
			
		}
	}
	
	
	@Test(timeout=5000)
	public void the2NodesShouldAbleToSendMessages() throws Throwable {
		int basePort = 10300;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 2; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "1")
					.setProperty("openkad.bucket.kbuckets.maxsize", "1")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
			
		System.out.println("finished joining");
		final AtomicBoolean isDone = new AtomicBoolean(false);
		kbrs.get(1).register("tag", new DefaultMessageHandler() {
			
			@Override
			public void onIncomingMessage(Node from, String tag, byte[] content) {
				Assert.assertEquals("msg", new String(content));
				synchronized (isDone) {
					isDone.set(true);
					isDone.notifyAll();
				}
			}
		});
		
		List<Node> findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey(), 5);
		
		kbrs.get(0).sendMessage(findNode.get(0), "tag", "msg".getBytes());
		
		synchronized (isDone) {
			while (!isDone.get())
				isDone.wait();
		}
	}
	
	@Test(timeout=30000)
	public void the16NodesShouldAbleToSendMessages() throws Throwable {
		int basePort = 10400;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 16; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "5")
					.setProperty("openkad.bucket.kbuckets.maxsize", "5")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
			
		System.out.println("finished joining");
		final AtomicBoolean isDone = new AtomicBoolean(false);
		kbrs.get(13).register("tag", new DefaultMessageHandler() {
			
			@Override
			public void onIncomingMessage(Node from, String tag, byte[] content) {
				Assert.assertEquals("msg", new String(content));
				System.out.println("got "+new String(content));
				synchronized (isDone) {
					isDone.set(true);
					isDone.notifyAll();
				}
			}
		});
		
		List<Node> findNode = kbrs.get(0).findNode(kbrs.get(13).getLocalNode().getKey(), 5);
		
		kbrs.get(2).sendMessage(findNode.get(0), "tag", "msg".getBytes());
		
		synchronized (isDone) {
			while (!isDone.get())
				isDone.wait();
		}
	}
	
	
	@Test(timeout=5000)
	public void the2NodesShouldAbleToSendRequest() throws Throwable {
		int basePort = 10500;
		List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>();
		for (int i=0; i < 2; ++i) {
			Injector injector = Guice.createInjector(new KadNetModule()
					.setProperty("openkad.keyfactory.keysize", "1")
					.setProperty("openkad.bucket.kbuckets.maxsize", "1")
					.setProperty("openkad.seed", ""+(i+basePort))
					.setProperty("openkad.net.udp.port", ""+(i+basePort)));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		for (int i=1; i < kbrs.size(); ++i) {
			int port = basePort + i -1;
			System.out.println(i+" ==> "+(i-1));
			kbrs.get(i).join(Arrays.asList(new URI("openkad.udp://127.0.0.1:"+port+"/")));
		}
			
		System.out.println("finished joining");
		
		kbrs.get(1).register("tag", new DefaultMessageHandler() {
			@Override
			public byte[] onIncomingRequest(Node from, String tag, byte[] content) {
				Assert.assertEquals("msg", new String(content));
				return "new_msg".getBytes();
			}
		});
		
		List<Node> findNode = kbrs.get(0).findNode(kbrs.get(1).getLocalNode().getKey(), 5);
		
		byte[] res = kbrs.get(0).sendRequest(findNode.get(0), "tag", "msg".getBytes()).get();
		Assert.assertEquals("new_msg", new String(res));
	}
}
