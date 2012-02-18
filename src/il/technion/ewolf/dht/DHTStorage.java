package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.Set;

public interface DHTStorage {

	public void store(Key key, long age, Serializable data);
	
	public Set<Serializable> search(Key key);
	
	public void setDHTName(String name);
		
	
}
