package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyHolder;

import java.math.BigInteger;
import java.util.Comparator;



public class KadKeyComparator implements Comparator<KeyHolder> {

	private final BigInteger key;
	
	public KadKeyComparator(Key key) {
		this.key = key.getInt();
	}
	
	@Override
	public int compare(KeyHolder n1, KeyHolder n2) {
		BigInteger b1 = n1.getKey().getInt();
		BigInteger b2 = n2.getKey().getInt();
		
		b1 = b1.xor(key);
		b2 = b2.xor(key);
		
		if (b1.signum() == -1 && b2.signum() != -1)
			return 1;
		if (b1.signum() != -1 && b2.signum() == -1)
			return -1;
		
		return b1.abs().compareTo(b2.abs());
		
	}

}
