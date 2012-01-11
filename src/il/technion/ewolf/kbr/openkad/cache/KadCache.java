package il.technion.ewolf.kbr.openkad.cache;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

import java.util.List;

public interface KadCache {

	public void insert(Key key, List<Node> nodes);
	
	public List<Node> search(Key key);
	
	public void clear();
	
}
