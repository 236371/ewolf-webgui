package il.technion.ewolf.dht.msg;

import il.technion.ewolf.kbr.Key;

import com.google.inject.Inject;

/**
 * A request sent for the remote node's findValueHandler
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class FindValueRequest extends DHTMessage {

	private static final long serialVersionUID = 5847074618063887663L;
	
	private Key key;
	
	@Inject
	FindValueRequest() {
		
	}
	
	
	public FindValueRequest setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public Key getKey() {
		return key;
	}
	
}
