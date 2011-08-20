package il.technion.ewolf.dht;

import java.io.IOException;

import ch.lambdaj.function.convert.Converter;
import encoding.Base64;

public class Base64Converter implements Converter<String, byte[]> {
	
	@Override
	public byte[] convert(String s) {
		try {
			return Base64.decode(s);
		} catch (IOException e) {
		}
		return null;
	}

}
