package il.technion.ewolf.dht.op;

import il.technion.ewolf.dht.DHTStorage;
import il.technion.ewolf.dht.msg.StoreMessage;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.Node;

import java.io.Serializable;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class PutOperation {

	// state
	private String dhtName;
	private Key key;
	private Serializable data;
	private long age = 0;
	private DHTStorage storage;
	
	// dependencies
	private final KeybasedRouting kbr;
	private final Provider<StoreMessage> storeMessageProvider;
	private final String storeMessageHandlerName;
	
	
	@Inject
	PutOperation(
			KeybasedRouting kbr,
			Provider<StoreMessage> storeMessageProvider,
			@Named("dht.handlers.name.store") String storeMessageHandlerName) {
		
		this.kbr = kbr;
		this.storeMessageProvider = storeMessageProvider;
		this.storeMessageHandlerName = storeMessageHandlerName;
	}
	
	public PutOperation setAge(long age) {
		this.age = age;
		return this;
	}
	
	public PutOperation setData(Serializable data) {
		this.data = data;
		return this;
	}
	
	public PutOperation setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public PutOperation setDhtName(String dhtName) {
		this.dhtName = dhtName;
		return this;
	}
	
	public PutOperation setStorage(DHTStorage storage) {
		this.storage = storage;
		return this;
	}
	

	public void doPut() {
		if (data == null || key == null || dhtName == null || age == 0 || storage == null)
			throw new IllegalArgumentException("missing params in dht put operation");
		
		storage.store(key, age, data);
		
		List<Node> nodes = kbr.findNode(key);
		
		StoreMessage storeMessage = storeMessageProvider.get()
				.setContent(data)
				.setAge(age)
				.setKey(key);
		
		for (Node n : nodes) {
			try {
				kbr.sendMessage(n, dhtName + storeMessageHandlerName, storeMessage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
}
