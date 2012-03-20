package il.technion.ewolf.dht.handlers;

import il.technion.ewolf.dht.DHTStorage;
import il.technion.ewolf.dht.msg.StoreMessage;
import il.technion.ewolf.kbr.Node;

import java.io.Serializable;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Handles store messages: insert the given data item into the storage
 * @author eyal.kibbar@gmail.com
 *
 */
public class StoreMessageHandler extends AbstractDHTHandler {

	// dependencies
	private final String storeMessageHandlerName;
	
	// state
	private DHTStorage storage;
	
	@Inject
	StoreMessageHandler(
			@Named("dht.handlers.name.store") String storeMessageHandlerName) {
		this.storeMessageHandlerName = storeMessageHandlerName;
	}
	
	@Override
	public void onIncomingMessage(Node from, String tag, Serializable content) {
		try {
			StoreMessage msg = (StoreMessage)content;
			storage.store(msg.getKey(), msg.getAge(), msg.getData());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Serializable onIncomingRequest(Node from, String tag, Serializable content) {
		throw new UnsupportedOperationException("only accepts messages");
	}

	@Override
	protected String getName() {
		return storeMessageHandlerName;
	}

	public StoreMessageHandler setStorage(DHTStorage storage) {
		this.storage = storage;
		return this;
	}

}
