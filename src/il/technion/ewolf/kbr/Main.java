package il.technion.ewolf.kbr;

import il.technion.ewolf.kbr.openkad.KadKeyComparator;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


import com.google.inject.Guice;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		
		props.setProperty("kadnet.keyfactory.keysize", "5");
		props.setProperty("kadnet.localkey", "an5vkzg=");
		
		KeybasedRouting kbr = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
		
		KeyFactory kf = kbr.getKeyFactory();
		
		Key k1 = kf.getFromKey("zikEXAE=");
		Key k2 = kf.getFromKey("y+Qa030=");
		List<Key> nodes = new ArrayList<Key>();
		nodes.add(k2);
		nodes.add(k1);
		System.out.println(nodes);
		Collections.sort(nodes, new KadKeyComparator(k1));
		System.out.println(nodes);
		/*
		kbr.create();
		
		kbr.join(new URI("otcpkad://ds-is16:10000/")).get();
		
		List<Node> nodes = kbr.findNodes(kf.getFromData("zikEXAE="), 5).get();
		
		System.out.println(nodes);
		Collections.sort(nodes, new KadKeyComparator(kf.getFromData("zikEXAE=")));
		System.out.println(nodes);
		*/
	}

}
