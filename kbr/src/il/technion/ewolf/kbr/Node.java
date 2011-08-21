package il.technion.ewolf.kbr;

import java.io.Serializable;


public abstract class Node implements Serializable {
	
	private static final long serialVersionUID = 4141864004771700615L;
	
	private final Key key;
	
	protected Node(Key key) {
		this.key = key;
	}
	
	public Key getKey() {
		return key;
	}
	
	
}
