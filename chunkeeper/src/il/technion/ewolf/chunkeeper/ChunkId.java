package il.technion.ewolf.chunkeeper;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

/**
 * Uniquely identifies a chunk. Holds the size of the data, the key
 * it is mapped by and the data's hash
 * @author eyal.kibbar@gmail.com
 *
 */
public class ChunkId implements Serializable {

	private static final long serialVersionUID = 7215790794713185534L;
	
	private Key key;
	private long size;
	private Map<String, byte[]> hashFromAlgo;

	
	ChunkId() {
		
	}
	
	public ChunkId(Key key, byte[] data, String ... algoNames) {
		this.key = key;
		this.size = data.length;
		this.hashFromAlgo = new HashMap<String, byte[]>();
		try {
			calcHash(data, algoNames);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	
	private void calcHash(byte[] data, String ... algoNames) throws NoSuchAlgorithmException {
		if (algoNames.length == 0)
			throw new IllegalArgumentException("no algos were given");
		
		for (String algoName : algoNames) {
			MessageDigest md = MessageDigest.getInstance(algoName);
			hashFromAlgo.put(algoName, md.digest(data));
		}
	}
	/**
	 * Get the key this data is mapped by
	 * @return
	 */
	public Key getKey() {
		return key;
	}
	
	/**
	 * Get the data size in bytes
	 * @return data size in bytes
	 */
	public long getSize() {
		return size;
	}
	
	/**
	 * Get the data's hash from a hash algorithm. May return null
	 * if no hash was found for the requested algorithm
	 * @param hashAlgo
	 * @return
	 */
	public byte[] getHash(String hashAlgo) {
		return hashFromAlgo.get(hashAlgo);
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		ChunkId other = (ChunkId)obj;
		
		if (getSize() != other.getSize())
			return false;
		
		if (!getKey().equals(other.getKey()))
				return false;
		
		boolean didCheck = false;
		for (String algoName : hashFromAlgo.keySet()) {
			byte[] myHash = hashFromAlgo.get(algoName);
			byte[] otherHash = other.getHash(algoName);
			
			if (otherHash == null)
				continue;
			
			didCheck = true;
			if (!Arrays.equals(myHash, otherHash))
				return false;
		}
		
		return didCheck;
	}
	
	@Override
	public String toString() {
		Map<String, String> algo = new HashMap<String, String>();
		for (String a : hashFromAlgo.keySet()) {
			algo.put(a, Base64.encodeBase64String(hashFromAlgo.get(a)));
		}
		return "Key: "+key.toBase64()+", size: "+size+", hash: "+algo;
	}
}
