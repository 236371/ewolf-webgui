package dht.openkad;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import dht.Key;
import dht.KeyFactory;

public class TestKeyFactory implements KeyFactory {

	private final int keyByteLength;
	private final Random rnd;
	
	public TestKeyFactory(int keyByteLength, long seed) {
		this.keyByteLength = keyByteLength;
		rnd = seed == 0 ? new Random() : new Random(seed);
	}
	
	@Override
	public Key generate() {
		return new Key(keyByteLength, rnd);
	}

	@Override
	public Key generate(BigInteger minSize, BigInteger maxSize) {
		return new Key(keyByteLength, rnd, minSize, maxSize);
	}

	@Override
	public Key getFromData(String data) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
			md.update(data.getBytes());
			return new Key(Arrays.copyOf(md.digest(), keyByteLength));
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public Key getFromKey(String hash) {
		try {
			if (Base64.decode(hash).length != keyByteLength) {
				throw new IllegalArgumentException("wrong key hash: "+hash);
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return new Key(hash);
	}

	@Override
	public Key getFromInt(BigInteger i) {
		byte[] b = new byte[keyByteLength];
		byte[] k = i.toByteArray();
		Arrays.fill(b, (byte)0);
		for (int n=Math.max(0, b.length-k.length), j=0;
			 j < k.length && n < b.length; ++n, ++j) {
			b[n] = k[j];
		}
		return new Key(b);
	}

	@Override
	public int getBitCount() {
		return keyByteLength*8;
	}

	@Override
	public int getByteCount() {
		return keyByteLength;
	}

}
