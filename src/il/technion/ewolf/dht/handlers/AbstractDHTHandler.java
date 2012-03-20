package il.technion.ewolf.dht.handlers;

import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.MessageHandler;

/**
 * Base class for all incoming message handlers
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public abstract class AbstractDHTHandler implements MessageHandler {

	protected AbstractDHTHandler() {
		
	}
	
	/**
	 * @return the handler name (such as "storeMessageHandler")
	 */
	protected abstract String getName(); 
	
	/**
	 * Register this handler to recv incoming messages from the
	 * key based routing
	 * @param dhtName associate this handler with a dht according to its name
	 * @param kbr the key based routing network
	 */
	public void register(String dhtName, KeybasedRouting kbr) {
		kbr.register(dhtName + getName(), this);
	}

	
}
