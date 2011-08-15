package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DHTTest {

	private List<KeybasedRouting> kbrs = new ArrayList<KeybasedRouting>(); 
	
	
	private void createNetwork(int size) throws IOException {
		Assert.assertTrue(kbrs.isEmpty());
		
		for (int i=0; i < size; ++i) {
			Properties props = new Properties();
			props.setProperty("kadnet.otcpkad.port", ""+(10000+i));
			Injector injector = Guice.createInjector(new KadNetModule(props));
			KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
			kbr.create();
			kbrs.add(kbr);
		}
		
		try {
			Thread.sleep(size * 100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Random rnd = new Random();
		for (int i=1; i < size; ++i) {
			int port = 10000+rnd.nextInt(i);
			try {
				kbrs.get(i).join(new URI("otcpkad://localhost:"+port+"/")).get();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (Exception e) {
				System.err.println("error joining from "+(i+10000)+" to "+port+" trying again .... ");
				try { Thread.sleep(100); } catch (InterruptedException e1) {}
				--i;
			}
		}
		
	}
	
	@After
	public void shutdownNetwork() {
		for (KeybasedRouting kbr : kbrs) {
			kbr.shutdown();
		}
	}
	
	private List<DHT> createDHTs(Properties props) {
		List<DHT> dhts = new ArrayList<DHT>();
		for (KeybasedRouting kbr : kbrs) {
			Injector injector = Guice.createInjector(new DHTModule(kbr, props));
			DHT dht = injector.getInstance(DHT.class);
			dht.start();
			dhts.add(dht);
		}
		return dhts;
	}
	
	@Test
	public void putWith2Nodes() throws Exception {
		createNetwork(2);
		Properties props = new Properties();
		props.setProperty("dht.replication.factor", "1");
		props.setProperty("dht.replication.saftymargin", "1");
		List<DHT> dhts = createDHTs(props);
		
		byte[] data = "XYZ".getBytes();
		dhts.get(0).put("abc", data).get();
		
		Thread.sleep(1000);
		
		List<byte[]> res = null;
		
		res = dhts.get(0).get("abc").get();
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(new String(data), new String(res.get(0)));
		
		res = dhts.get(1).get("abc").get();
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(new String(data), new String(res.get(0)));
	}
	
	
	@Test
	public void putWith16Nodes() throws Exception {
		createNetwork(16);
		Properties props = new Properties();
		props.setProperty("dht.replication.factor", "1");
		props.setProperty("dht.replication.saftymargin", "1");
		List<DHT> dhts = createDHTs(props);
		
		byte[] data = "XYZ".getBytes();
		dhts.get(0).put("abc", data).get();
		
		Thread.sleep(1000);
		
		List<byte[]> res = null;
		
		res = dhts.get(9).get("abc").get();
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(new String(data), new String(res.get(0)));
		
		res = dhts.get(15).get("abc").get();
		Assert.assertEquals(1, res.size());
		Assert.assertEquals(new String(data), new String(res.get(0)));
	}
	
	
	@Test
	public void put2ValuesWith16Nodes() throws Exception {
		createNetwork(16);
		Properties props = new Properties();
		props.setProperty("dht.replication.factor", "1");
		props.setProperty("dht.replication.saftymargin", "1");
		List<DHT> dhts = createDHTs(props);
		
		byte[] data1 = "XYZ".getBytes();
		byte[] data2 = "UVW".getBytes();
		
		dhts.get(0).put("abc", data1).get();
		dhts.get(3).put("abc", data2).get();
		
		Thread.sleep(1000);
		
		List<byte[]> res = null;
		
		res = dhts.get(9).get("abc").get();
		Assert.assertEquals(2, res.size());
		Assert.assertTrue(Arrays.equals(data1, res.get(0)) || Arrays.equals(data1, res.get(1)));
		Assert.assertTrue(Arrays.equals(data2, res.get(0)) || Arrays.equals(data2, res.get(1)));
		
		res = dhts.get(15).get("abc").get();
		Assert.assertEquals(2, res.size());
		Assert.assertTrue(Arrays.equals(data1, res.get(0)) || Arrays.equals(data1, res.get(1)));
		Assert.assertTrue(Arrays.equals(data2, res.get(0)) || Arrays.equals(data2, res.get(1)));
	}
}
