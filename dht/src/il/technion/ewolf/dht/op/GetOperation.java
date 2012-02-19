package il.technion.ewolf.dht.op;

import il.technion.ewolf.dht.DHTStorage;
import il.technion.ewolf.dht.msg.FindValueRequest;
import il.technion.ewolf.dht.msg.FindValueResponse;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class GetOperation {

	// state
	private String dhtName;
	private Key key;
	private DHTStorage storage;
	
	// dependencies
	private final KeybasedRouting kbr;
	private final Provider<FindValueRequest> findValueRequestProvider;
	private final String findValueRequestHandlerName;
	
	
	
	@Inject
	GetOperation(
			KeybasedRouting kbr,
			Provider<FindValueRequest> findValueRequestProvider,
			@Named("dht.handlers.name.findvalue") String findValueRequestHandlerName) {
		
		this.kbr = kbr;
		this.findValueRequestProvider = findValueRequestProvider;
		this.findValueRequestHandlerName = findValueRequestHandlerName;
	}
	
	public GetOperation setDhtName(String dhtName) {
		this.dhtName = dhtName;
		return this;
	}
	
	public GetOperation setStorage(DHTStorage storage) {
		this.storage = storage;
		return this;
	}
	
	public GetOperation setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public List<Serializable> doGet() {
		if (dhtName == null || storage == null || key == null)
			throw new IllegalArgumentException("missing param in dht get operation");
		
		List<Node> nodes = kbr.findNode(key);
		//System.out.println("key: "+key+" nodes: "+nodes);
		FindValueRequest findValueRequest = findValueRequestProvider.get()
				.setKey(key);
		
		
		final Set<Serializable> results = Collections.synchronizedSet(new HashSet<Serializable>());
		final CountDownLatch latch = new CountDownLatch(nodes.size());
		
		for (Node n : nodes) {
			//System.out.println("sending to "+n);
			kbr.sendRequest(n, dhtName + findValueRequestHandlerName, findValueRequest, null, new CompletionHandler<Serializable, Void>() {
				
				@Override
				public void completed(Serializable res, Void nothing) {
					try {
						FindValueResponse findValueResponse = (FindValueResponse)res;
						results.addAll(findValueResponse.getValues());
					} finally {
						latch.countDown();
					}
				}
				
				@Override
				public void failed(Throwable exc, Void nothing) {
					exc.printStackTrace();
					latch.countDown();
				}
			});
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Set<Serializable> localResults = storage.search(key);
		if (localResults != null)
			results.addAll(localResults);
		
		return new ArrayList<Serializable>(results);
	}


	

}
