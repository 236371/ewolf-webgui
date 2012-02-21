package il.technion.ewolf.dht.handlers;

import il.technion.ewolf.dht.DHTStorage;
import il.technion.ewolf.dht.msg.FindValueRequest;
import il.technion.ewolf.dht.msg.FindValueResponse;
import il.technion.ewolf.kbr.Node;

import java.io.Serializable;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Handle the find value requests: search the storage for the
 * requested key and sends the results back to the requester
 * 
 * @author eyal.kibbar@gmail.com
 */
public class FindvalueHandler extends AbstractDHTHandler {

	// state
	private DHTStorage storage;

	// dependencies
	private final String findvalueHandlerName;
	private final Provider<FindValueResponse> findValueResponseProvider;
	
	@Inject
	FindvalueHandler(
			@Named("dht.handlers.name.findvalue") String findvalueHandlerName,
			Provider<FindValueResponse> findValueResponseProvider) {
		
		this.findvalueHandlerName = findvalueHandlerName;
		this.findValueResponseProvider = findValueResponseProvider;
	}
	
	public FindvalueHandler setStorage(DHTStorage storage) {
		this.storage = storage;
		return this;
	}
	
	@Override
	public void onIncomingMessage(Node from, String tag, Serializable content) {
		throw new UnsupportedOperationException("find value only accepts requests");
	}

	@Override
	public Serializable onIncomingRequest(Node from, String tag, Serializable content) {
		FindValueRequest req = (FindValueRequest)content;
		Set<Serializable> searchResults = storage.search(req.getKey());
		
		return findValueResponseProvider.get()
				.setValues(searchResults);
	}

	@Override
	protected String getName() {
		return findvalueHandlerName;
	}

}
