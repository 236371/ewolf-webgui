package il.technion.ewolf.dht;

import il.technion.ewolf.dht.handlers.FindvalueHandler;
import il.technion.ewolf.dht.handlers.StoreMessageHandler;
import il.technion.ewolf.dht.op.GetOperation;
import il.technion.ewolf.dht.op.PutOperation;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;

import java.io.Serializable;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SimpleDHT implements DHT {

	// dependencies
	private final KeybasedRouting kbr;
	private final FindvalueHandler findvalueHandler;
	private final StoreMessageHandler storeMessageHandler;
	private final Provider<GetOperation> getOperationProvider;
	private final Provider<PutOperation> putOperationProvider;
	
	// state
	private String dhtName = null;
	private DHTStorage storage = null;
	
	@Inject
	SimpleDHT(KeybasedRouting kbr,
			
		FindvalueHandler findvalueHandler,
		StoreMessageHandler storeMessageHandler,
		
		Provider<GetOperation> getOperationProvider,
		Provider<PutOperation> putOperationProvider) {
		
		this.kbr = kbr;
		
		this.findvalueHandler = findvalueHandler;
		this.storeMessageHandler = storeMessageHandler;
		
		this.getOperationProvider = getOperationProvider;
		this.putOperationProvider = putOperationProvider;
	}
	
	public SimpleDHT setName(String dhtName) {
		this.dhtName = dhtName;
		return this;
	}
	
	public SimpleDHT setStorage(DHTStorage storage) {
		this.storage = storage;
		return this;
	}
	
	public SimpleDHT create() {
		if (dhtName == null || storage == null)
			throw new IllegalStateException("missing name or storage");
		
		storage.setDHTName(dhtName);
		findvalueHandler
			.setStorage(storage)
			.register(dhtName, kbr);
		
		storeMessageHandler
			.setStorage(storage)
			.register(dhtName, kbr);
		
		return this;
	}

	public void put(Serializable data, String ... tags) {
		put(kbr.getKeyFactory().create(tags), data);
	}
	
	public void put(Key key, Serializable data) {
		putOperationProvider.get()
			.setKey(key)
			.setData(data)
			.setDhtName(dhtName)
			.setStorage(storage)
			.setAge(System.currentTimeMillis())
			.doPut();
	}
	
	public List<Serializable> get(String ... tags) {
		return get(kbr.getKeyFactory().create(tags));
	}
	
	public List<Serializable> get(Key key) {
		return getOperationProvider.get()
			.setKey(key)
			.setDhtName(dhtName)
			.setStorage(storage)
			.doGet();
	}
	
}
