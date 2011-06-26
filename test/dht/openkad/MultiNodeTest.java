package dht.openkad;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import dht.DHT;
import dht.Key;
import dht.KeyFactory;

public class MultiNodeTest {

	private final int basePort = 10000;
	private final Random rnd = new Random(0);
	
	@Test//(timeout=60000)
	public void test() throws Exception {
		
		List<DHT> nodes = new ArrayList<DHT>();
		
		// create all nodes
		for (int i=0; i < 10; ++i) {
			Properties prop = new Properties();
			prop.setProperty("kad.endpoint.tcpkad.port", Integer.toString((basePort + i)));
			prop.setProperty("kad.keyfactory.seed", Integer.toString(i+1));
			Injector injector = Guice.createInjector(new MultiNodeTestModule(prop));
			DHT kad = injector.getInstance(DHT.class);
			kad.create();
			nodes.add(kad);
		}
		Thread.sleep(1000);
		
		// join into a random network
		for (int i=1; i < nodes.size(); ++i) {
			int r = rnd.nextInt(i);
			URI uri = new URI("tcpkad://127.0.0.1:"+(basePort+r)+"/"+nodes.get(r).getNodeID());
			nodes.get(i).join(uri);
		}
		Thread.sleep(1000);
		
		
		KeyFactory kf = nodes.get(0).getKeyFactory();
		final Key k1 = kf.generate();
		String val1 = "value 1";
		List<DHT> sortedNodes = new ArrayList<DHT>(nodes);
		Collections.sort(sortedNodes, new Comparator<DHT>() {

			private final BigInteger key = k1.getInt();
			
			@Override
			public int compare(DHT n1, DHT n2) {
				BigInteger b1 = n1.getNodeID().getInt();
				BigInteger b2 = n2.getNodeID().getInt();
				
				b1 = b1.xor(key);
				b2 = b2.xor(key);
				if (b1.signum() == -1 && b2.signum() != -1)
					return 1;
				if (b1.signum() != -1 && b2.signum() == -1)
					return -1;
				
				return b1.abs().compareTo(b2.abs());
			}
		});
		Thread.sleep(1000);
		
		
		// Do PUT !!!
		sortedNodes.get(5).put(k1, val1).get();
		System.out.println("put: "+sortedNodes.get(5).getNodeID());
		Thread.sleep(1000);
		
		
		/*
		for (DHT n : sortedNodes) {
			System.out.print("localGet "+n.getNodeID()+": ");
			System.out.println(n.localGet(k1));
		}
		*/
		
	}
	
}
