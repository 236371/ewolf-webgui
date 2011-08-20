package il.technion.ewolf.kbr;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import encoding.Base64;

/**
 * The Key class represents nodes in our network, every node has 
 * a unique key and nodes can be looked up using their keys.
 * 
 * @author eyal
 *
 */
public class Key implements Serializable {

	private static final long serialVersionUID = 1136956138268529759L;
	
	private final String hash;
	
	
	public Key(int nrBytes, Random rnd) {
		byte[] b = new byte[nrBytes];
		rnd.nextBytes(b);
		hash = Base64.encodeBytes(b);
	}
	
	public Key(int nrBytes, Random rnd, BigInteger minSize, BigInteger maxSize) {
		if (maxSize.bitCount() > nrBytes*8)
			throw new IllegalArgumentException();
		
		byte[] b = new byte[nrBytes];
		rnd.nextBytes(b);
		BigInteger k = minSize.add(new BigInteger(b).mod(maxSize.subtract(minSize)));
		byte[] bb =k.toByteArray();
		
		Arrays.fill(b, (byte)0);
		for (int i=nrBytes-1, j=bb.length-1; j >= 0 && i >= 0; --i, --j) {
			b[i] = bb[j];
		}
			
		hash = Base64.encodeBytes(b);
	}
	
	public Key(byte[] bytes) {
		hash = Base64.encodeBytes(bytes);
	}
	
	public Key(String data, String algo) throws NoSuchAlgorithmException {
		hash = hash(data, algo);
	}
	
	public Key(String hash) {
		this.hash = hash;
	}
	
	public String toBase64() {
		return hash;
	}
	
	public BigInteger getInt() {
		return new BigInteger(getBytes());
	}
	
	
	
	public byte[] getBytes() {
		try {
			return Base64.decode(hash);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	public boolean equals(Object o) {
		if (o == null || !(o.getClass().equals(this.getClass())))
			return false;
		return hash.equals(((Key)o).hash);
	}
	
	public int hashCode() {
		return hash.hashCode();
	}
	
	private String hash(String k, String algo) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algo);
		md.update(k.getBytes());
		return Base64.encodeBytes(md.digest());
	}
	
	public String toString() {
		return hash;
	}

}
