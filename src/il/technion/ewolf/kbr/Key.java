package il.technion.ewolf.kbr;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;



public class Key implements Serializable {

	private static final long serialVersionUID = 4137662182397711129L;
	
	private transient byte[] bytes = null;
	
	private final String base64Encoded;

	
	Key(byte[] bytes) {
		this.bytes = bytes;
		base64Encoded = Base64.encodeBase64String(bytes);
	}
	
	public boolean isZeroKey() {
		for (byte x : getBytes()) {
			if (x != 0)
				return false;
		}
		return true;
	}
	
	public int getColor(int nrColors) {
		return Math.abs(getInt().intValue()) % nrColors;
	}
	
	public byte[] getBytes() {
		if (bytes == null)
			bytes = Base64.decodeBase64(base64Encoded);
		return bytes;
	}
	
	public int getByteLength() {
		return getBytes().length;
	}
	
	public Key xor(Key k) {
		if (k.getByteLength() != getByteLength())
			throw new IllegalArgumentException("incompatable key for xor");
		byte[] b = new byte[getByteLength()];
		for (int i=0; i < b.length; ++i) {
			b[i] = (byte)(getBytes()[i] ^ k.getBytes()[i]);
		}
		return new Key(b);
	}
	
	public int getFirstSetBitIndex() {
		for (int i=0; i < getByteLength(); ++i) {
			if (getBytes()[i] == 0) 
				continue;
			
			int j;
			for (j=7; (getBytes()[i] & (1 << j)) == 0; --j);
			return (getByteLength()-i-1)*8+j;
		}
		return -1;
	}
	
	public int getBitLength() {
		return getByteLength() * 8;
	}
	
	public BigInteger getInt() {
		return new BigInteger(getBytes());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !getClass().equals(o.getClass()))
			return false;
		return base64Encoded.equals(((Key)o).base64Encoded);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(getBytes());
	}
	
	public String toBase64() {
		return base64Encoded;
	}
	
	public String toString() {
		return base64Encoded;
	}
	
	public String toBinaryString() {
		String $ = "";
		for (int i=0; i < getByteLength(); ++i) {
			byte b = getBytes()[i];
			// fix negative numbers
			$ += b < 0 ? "1" : "0";
			b &= 0x7F;
			
			// fix insufficient leading 0s
			String str = Integer.toBinaryString(b);
			switch (str.length()) {
			case 1: $ += "000000"; break;
			case 2: $ += "00000"; break;
			case 3: $ += "0000"; break;
			case 4: $ += "000"; break;
			case 5: $ += "00"; break;
			case 6: $ += "0"; break;
			}
			$ += str+" ";
		}
		return $;
	}
}
