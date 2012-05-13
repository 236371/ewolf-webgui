package il.technion.ewolf.server;

import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.http.HttpConnectorModule;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class EwolfServer {
	
	final static Object lock = new Object();


	public static void main(String[] args) {
		int basePort = 10200;

		Injector injector = Guice.createInjector(
				new HttpConnectorModule()
					.setProperty("httpconnector.net.port", ""+(basePort)),
				
				new KadNetModule()
				.setProperty("openkad.keyfactory.keysize", "1")
				.setProperty("openkad.bucket.kbuckets.maxsize", "3")
				.setProperty("openkad.seed", ""+(basePort))
				.setProperty("openkad.net.udp.port", ""+(basePort)));
		
		HttpConnector connector = injector.getInstance(HttpConnector.class);
		connector.bind();
		connector.start();
	
		
		System.out.println("finished joining");		
		
		connector.register("/json*", new JsonHandler() );
		new HttpFileHandler("/", "*", new ServerResourceFactory(), connector);
		
		System.out.println("server started");
		
		
		synchronized (lock) {
			while(true)
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}

	}

}
