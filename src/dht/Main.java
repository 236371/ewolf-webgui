package dht;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;

import dht.openkad.KademliaModule;



public class Main {

	public static void main(String[] args) throws Exception{
		
		Properties prop = new Properties();
		//prop.setProperty("kad.endpoint.tcpkad.port", args[0]);
		//prop.setProperty("kad.keyfactory.seed", args[1]);
		Injector injector = Guice.createInjector(new KademliaModule(prop));
		DHT kad = injector.getInstance(DHT.class);
		kad.create();
		
		/*
		if (args.length >= 3) {
			kad.join(new URI(args[2]));
		} else {
			kad.create();
		}
		*/
		
	}

}
