package il.technion.ewolf.dht;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.List;

public interface DHT {
	
	public DHT setName(String dhtName);
	
	public DHT setStorage(DHTStorage storage);
	
	public DHT create();

	public void put(Serializable data, String ... tags);
	
	public void put(Key key, Serializable data);
	
	public List<Serializable> get(String ... tags);
	
	public List<Serializable> get(Key key);
	
}
