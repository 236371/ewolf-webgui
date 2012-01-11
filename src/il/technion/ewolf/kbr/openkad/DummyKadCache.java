package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.cache.KadCache;

import java.util.List;

public class DummyKadCache implements KadCache {

	@Override
	public void insert(Key key, List<Node> nodes) {
	}

	@Override
	public List<Node> search(Key key) {
		return null;
	}

	@Override
	public void clear() {
	}

}
