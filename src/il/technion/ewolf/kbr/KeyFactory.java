package il.technion.ewolf.kbr;

import java.io.IOException;
import java.io.InputStream;

public interface KeyFactory {
	
	public Key getZeroKey();
	public Key generate();
	public Key generate(int pow2Max);
	public Key get(byte[] bytes);
	public Key get(String base64Encoded);
	public Key create(String ... topics);
	public Key create(InputStream data) throws IOException;
	public int getByteLength();
	public int getBitLength();
	public boolean isValid(Key key);
	
}
