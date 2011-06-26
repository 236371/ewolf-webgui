package dht;

import java.math.BigInteger;

public interface KeyFactory {
	
	public Key generate();
	public Key generate(BigInteger minSize, BigInteger maxSize);
	public Key getFromData(String data);
	public Key getFromKey(String hash);
	public Key getFromInt(BigInteger i);
	public int getBitCount();
	public int getByteCount();
}
