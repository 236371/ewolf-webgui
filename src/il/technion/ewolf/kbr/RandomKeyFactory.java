package il.technion.ewolf.kbr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

public class RandomKeyFactory implements KeyFactory {

	private final int keyByteLength;
	private final Random rnd;
	private final MessageDigest md;
	
	
	public RandomKeyFactory(int keyByteLength, Random rnd, String hashAlgo) throws NoSuchAlgorithmException {
		this.keyByteLength = keyByteLength;
		this.rnd = rnd;
		this.md = MessageDigest.getInstance(hashAlgo);
	}
	
	@Override
	public Key getZeroKey() {
		byte[] b = new byte[keyByteLength];
		Arrays.fill(b, (byte)0);
		return new Key(b);
	}
	
	@Override
	public Key generate() {
		byte[] b = new byte[keyByteLength];
		rnd.nextBytes(b);
		return new Key(b);
	}
	
	@Override
	public Key generate(int pow2Max) {
		
		if (pow2Max < 0 || keyByteLength*8 <= pow2Max)
			throw new IllegalArgumentException();
		
		byte[] b = new byte[keyByteLength];
		Arrays.fill(b, (byte)0);
		
		byte[] r = new BigInteger(pow2Max, rnd).toByteArray();
		
		for (int i_b=b.length-1, i_r=r.length-1; i_r >=0 && i_b >= 0; b[i_b--] = r[i_r--]);
		
		b[b.length - pow2Max/8 - 1] |= 1 << (pow2Max % 8);
		
		return new Key(b);
	}

	@Override
	public Key get(byte[] bytes) {
		if (bytes.length != keyByteLength)
			throw new IllegalArgumentException("key length is invalid");
		return new Key(bytes);
	}

	@Override
	public Key get(String base64Encoded) {
		byte[] decodeBase64 = Base64.decodeBase64(base64Encoded);
		if (decodeBase64.length != keyByteLength)
			throw new IllegalArgumentException("key length is invalid");
		return new Key(decodeBase64);
	}
	

	@Override
	public Key create(String ... topics) {
		
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		
		try {
			out = new ByteArrayOutputStream();
			for (String topic : topics)
				out.write(topic.getBytes());
			
			out.flush();
			
			in = new ByteArrayInputStream(out.toByteArray());
			
			return create(in);
			
		}catch (IOException e) {
			throw new AssertionError();
		} finally {
			try { out.close(); } catch (Exception e) {}
			try { in.close(); } catch (Exception e) {}
		}
	}
	
	@Override
	public Key create(InputStream data) throws IOException {
		byte[] buff = new byte[512];
		int n;
		while ((n = data.read(buff)) > 0) {
			md.update(buff, 0, n);
		}
		byte[] b = md.digest();
		if (b.length > keyByteLength) {
			b = Arrays.copyOfRange(b, 0, keyByteLength);
		}
		return new Key(b);
	}
	
	@Override
	public int getByteLength() {
		return keyByteLength;
	}
	
	@Override
	public int getBitLength() {
		return getByteLength() * 8;
	}

	@Override
	public boolean isValid(Key key) {
		return key.getByteLength() == getByteLength();
	}
	
}
