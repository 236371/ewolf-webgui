package il.technion.ewolf.dht.handlers;

import il.technion.ewolf.kbr.KeybasedRouting;
import il.technion.ewolf.kbr.MessageHandler;

public abstract class AbstractDHTHandler implements MessageHandler {

	protected AbstractDHTHandler() {
		
	}
	
	protected abstract String getName(); 
	
	public void register(String dhtName, KeybasedRouting kbr) {
		kbr.register(dhtName + getName(), this);
	}

	
}
