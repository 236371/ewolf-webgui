package dht.openkad;

import java.math.BigInteger;
import java.util.Comparator;

import dht.Key;

public class KadNodeComparator implements Comparator<KadNode> {

	private final BigInteger key;
	
	public KadNodeComparator(Key key) {
		this.key = key.getInt();
	}
	
	@Override
	public int compare(KadNode n1, KadNode n2) {
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
