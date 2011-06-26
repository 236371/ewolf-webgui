package dht.openkad;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import com.google.inject.Inject;

import dht.Key;
import dht.KeyFactory;

public class KadKeyFactory implements KeyFactory {

	private final Random rnd;
	
	private final String KadKeyHashAlgorithm = "SHA-1";
	private final int KadKeyByteLength = 20;
	
	
	public KadKeyFactory() {
		this(0);
	}
	
	@Inject
	KadKeyFactory(long seed) {
		rnd = seed == 0 ? new Random() : new Random(seed);
	}

	@Override
	public Key generate() {
		return new Key(KadKeyByteLength, rnd);
	}
	
	@Override
	public Key generate(BigInteger minSize, BigInteger maxSize) {
		return new Key(KadKeyByteLength, rnd, minSize, maxSize);
	}

	@Override
	public Key getFromData(String data) {
		try {
			return new Key(data, KadKeyHashAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public Key getFromKey(String hash) {
		try {
			if (Base64.decode(hash).length != KadKeyByteLength) {
				throw new IllegalArgumentException("wrong key hash: "+hash);
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		return new Key(hash);
	}

	@Override
	public Key getFromInt(BigInteger i) {
		byte[] b = new byte[KadKeyByteLength];
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
		return KadKeyByteLength * 8;
	}

	@Override
	public int getByteCount() {
		return KadKeyByteLength;
	}


	
}
