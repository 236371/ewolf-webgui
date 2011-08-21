package il.technion.ewolf.kbr;

import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {

	/**
	 * @param args
	 */

	public static void main(String[] args) throws Exception {
		Properties props1 = new Properties();
		Properties props2 = new Properties();
		Injector injector;

		props1.setProperty("kadnet.otcpkad.port", "10001");
		injector = Guice.createInjector(new KadNetModule(props1));
		KeybasedRouting kbr1 = injector.getInstance(KeybasedRouting.class);
		kbr1.create();
		kbr1.register("some tag", new DefaultNodeConnectionListener() {
			@Override
			public void onIncomingMessage(String tag, Node from, InputStream in)
					throws IOException {
				byte[] b = new byte[4096];
				int n = in.read(b);
				System.out.println("Got message from " + from.getKey());
				System.out.println(new String(Arrays.copyOf(b, n)));
			}
		});

		props2.setProperty("kadnet.otcpkad.port", "10002");
		injector = Guice.createInjector(new KadNetModule(props2));
		KeybasedRouting kbr2 = injector.getInstance(KeybasedRouting.class);
		kbr2.create();
		kbr2.register("some tag", new DefaultNodeConnectionListener() {
		});

		kbr2.join(new URI("otcpkad://127.0.0.1:10001/")).get();

		KeyFactory keyFactory = kbr1.getKeyFactory();
		Key k1 = keyFactory.getFromData("any arbitrary data");

		List<Node> nodes = kbr1.findNodes(k1, 10).get();

		// send message to all nodes
		for (Node n : nodes) {
			OutputStream out = kbr1.sendMessage(n, "some tag");
			out.write(("hello node " + n.getKey() + " !!").getBytes());
			out.close();
		}

	}

}
