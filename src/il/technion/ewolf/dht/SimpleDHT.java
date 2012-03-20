package il.technion.ewolf.dht;

import il.technion.ewolf.dht.handlers.FindvalueHandler;
import il.technion.ewolf.dht.handlers.StoreMessageHandler;
import il.technion.ewolf.dht.op.GetOperation;
import il.technion.ewolf.dht.op.PutOperation;
import il.technion.ewolf.dht.storage.AgeLimitedDHTStorage;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.openkad.KadNetModule;

import java.io.Serializable;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Implements a DHT in the most strait forward way.
 * put finds the nodes closest to the key using a key based routing and
 * sends store message to all the found nodes.
 * get finds the nodes closest to the key and sends them a get request.
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
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
	
	public static void main(String[] args) throws Exception {
		Injector injector = Guice.createInjector(
				new KadNetModule().setProperty("openkad.net.udp.port", "5555"),

				new SimpleDHTModule());

		KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		kbr.create();

		// create a storage
		DHTStorage storage = injector.getInstance(AgeLimitedDHTStorage.class)
				.create();

		// create the dht and register the storage class
		injector.getInstance(DHT.class).setName("dht")
				.setStorage(storage)
				.create();
	}
	
}
