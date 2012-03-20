package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.List;

public interface DHT {
	
	/**
	 * sets the name for the dht to associate different storage
	 * classes for different dhts
	 * 
	 * @param dhtName the dht name
	 * @return this for fluent interface
	 */
	public DHT setName(String dhtName);
	
	/**
	 * sets the storage class
	 * 
	 * @param storage the storage class
	 * @return this for fluent interface
	 */
	public DHT setStorage(DHTStorage storage);
	
	/**
	 * Register all the handlers for dht messages
	 * @return this for fluent interface
	 */
	public DHT create();

	/**
	 * put data into the dht mapped with tags
	 * @param data the data to be inserted
	 * @param tags used to retrieve the data later
	 */
	public void put(Serializable data, String ... tags);
	
	/**
	 * put data into the dht mapped by key
	 * @param key the data's key used to retrieve it later
	 * @param data the data to be inserted
	 */
	public void put(Key key, Serializable data);
	
	/**
	 * finds data in the dht from tags
	 * @param tags the tags used to insert the data 
	 * @return all data items mapped with the given tags
	 */
	public List<Serializable> get(String ... tags);
	
	/**
	 * finds data in the dht from a key
	 * @param key the key used to insert the data 
	 * @return all data items mapped with the given tags
	 */
	public List<Serializable> get(Key key);
	
}
