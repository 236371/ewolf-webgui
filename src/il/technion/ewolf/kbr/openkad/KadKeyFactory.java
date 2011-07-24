package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import encoding.Base64;


public class KadKeyFactory implements KeyFactory {

	private final int keyByteLength;
	private final Random rnd;
	private final MessageDigest md;
	
	public KadKeyFactory(int keyByteLength, long seed, String hashFuncName) throws NoSuchAlgorithmException {
		this.keyByteLength = keyByteLength;
		rnd = seed == 0 ? new Random() : new Random(seed);
		md = MessageDigest.getInstance(hashFuncName);
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
	public synchronized Key getFromData(String data) {
		md.reset();
		md.update(data.getBytes());
		return new Key(Arrays.copyOf(md.digest(), keyByteLength));
	}

	@Override
	public Key getFromKey(String hash) {
		try {
			if (Base64.decode(hash).length != keyByteLength) {
				throw new IllegalArgumentException("wrong key hash: "+hash+" xpecting len "+keyByteLength+" but got "+Base64.decode(hash).length);
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

	@Override
	public Key getFromData(InputStream in) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid(Key key) {
		return key.getBytes().length == getByteCount();
	}

}
