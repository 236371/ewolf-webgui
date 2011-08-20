package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;

import java.math.BigInteger;
import java.util.Comparator;



public class KadKeyComparator implements Comparator<Key> {

	private final BigInteger key;
	
	public KadKeyComparator(Key key) {
		this.key = key.getInt();
	}
	
	@Override
	public int compare(Key n1, Key n2) {
		BigInteger b1 = n1.getInt();
		BigInteger b2 = n2.getInt();
		
		b1 = b1.xor(key);
		b2 = b2.xor(key);
		
		if (b1.signum() == -1 && b2.signum() != -1)
			return 1;
		if (b1.signum() != -1 && b2.signum() == -1)
			return -1;
		
		return b1.abs().compareTo(b2.abs());
		
	}

}
