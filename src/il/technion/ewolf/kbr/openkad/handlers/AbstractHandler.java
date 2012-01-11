package il.technion.ewolf.kbr.openkad.handlers;

import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.MessageFilter;

import java.util.Collection;

import com.google.inject.Provider;

public abstract class AbstractHandler implements CompletionHandler<KadMessage, Void>{

	private final Provider<MessageDispatcher<Void>> msgDispatcherProvider;
	
	
	AbstractHandler(Provider<MessageDispatcher<Void>> msgDispatcherProvider) {
		this.msgDispatcherProvider = msgDispatcherProvider;
	}
	
	protected abstract Collection<MessageFilter> getFilters();
	
	public void register() {
		MessageDispatcher<Void> dispatcher = msgDispatcherProvider.get();
		
		for (MessageFilter filter : getFilters()) {
			dispatcher.addFilter(filter);
		}
		
		dispatcher
			.setConsumable(false)
			.setCallback(null, this)
			.register();
	}
}
