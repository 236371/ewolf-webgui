package il.technion.ewolf.kbr;

import il.technion.ewolf.kbr.openkad.KadKeyComparator;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


import com.google.inject.Guice;

public class KeyGenerator {

	

	public static void main(String[] args) throws Exception {
		String propsFilename = args[0];
		String outputFilename = args[1];
		
		Properties props = new Properties();
		props.load(new FileInputStream(propsFilename));
		
		KeybasedRouting kbr = Guice
				.createInjector(new KadNetModule(props))
				.getInstance(KeybasedRouting.class);
		
		List<Key> keys = new ArrayList<Key>();
		
		FileOutputStream out = new FileOutputStream(outputFilename);
		
		for (int i=0; i < 4000; ++i) {
			Key k = kbr.getKeyFactory().generate();
			keys.add(k);
			out.write(k.toBase64().getBytes());
			//out.write("\t".getBytes());
			//String encoded = URLEncoder.encode(k.toBase64(), "UTF8");
			//out.write(encoded.getBytes());
			out.write("\n".getBytes());
		}
		out.close();
		System.out.println("DONE !");
		Collections.sort(keys, new KadKeyComparator(keys.get(0)));
		System.out.println(keys);
		
	}



}
