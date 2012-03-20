package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.Set;

/**
 * This interface represent any (key,value) storage semantics
 * Different implementation may have different behaviors, such as
 * persistent or volatile, different valid time for items, max size
 * or even a reinsert policy.
 *   
 * @author eyal.kibbar@gmail.com
 *
 */
public interface DHTStorage {

	/**
	 * Store data under a given key
	 * @param key the key mapping the data
	 * @param age the data's creation date (in millies)
	 * @param data the data to be stored
	 */
	public void store(Key key, long age, Serializable data);
	
	/**
	 * Search data mapped by the given key
	 * @param key the data's key
	 * @return all data items found mapped with the given key
	 */
	public Set<Serializable> search(Key key);
	
	/**
	 * Sets the name of the storage.
	 * Used by the dht in the storage registration process 
	 * @param name
	 */
	public void setDHTName(String name);
		
	
}
