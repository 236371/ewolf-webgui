package il.technion.ewolf.dht;

import java.util.Arrays;

public class ByteArray {

	private byte[] bytes;
	private  int hash;
	
	
	public ByteArray(byte[] bytes) {
		this.bytes = bytes;
		this.hash = Arrays.hashCode(bytes);
		/*
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new AssertionError("This should never happen");
		}
		hash = md.digest(bytes);
		*/
	}
	
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !this.getClass().equals(obj.getClass()))
			return false;
		ByteArray b = (ByteArray)obj;
		if (b.hash != hash)
			return false;
		return Arrays.equals(getBytes(), b.getBytes());
	}
	
	public byte[] getBytes() {
		return bytes;
	}
}
