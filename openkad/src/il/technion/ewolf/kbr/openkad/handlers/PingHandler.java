package il.technion.ewolf.kbr.openkad.handlers;

import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.PingRequest;
import il.technion.ewolf.kbr.openkad.msg.PingResponse;
import il.technion.ewolf.kbr.openkad.net.KadServer;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.MessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeMessageFilter;

import java.util.Arrays;
import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Handles ping requests by sending back a ping response
 * @author eyal.kibbar@gmail.com
 *
 */
public class PingHandler extends AbstractHandler {

	private final KadServer kadServer;
	private final Node localNode;
	
	@Inject
	PingHandler(
			Provider<MessageDispatcher<Void>> msgDispatcherProvider,
			KadServer kadServer,
			@Named("openkad.local.node") Node localNode) {
		super(msgDispatcherProvider);
		this.kadServer = kadServer;
		this.localNode = localNode;
	}

	@Override
	public void completed(KadMessage msg, Void attachment) {
		try {
			PingResponse pingResponse = ((PingRequest)msg).generateResponse(localNode);
			kadServer.send(msg.getSrc(), pingResponse);
		} catch (Exception e) {
			// nothing to do
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// should never b here
	}

	@Override
	protected Collection<MessageFilter> getFilters() {
		return Arrays.asList(new MessageFilter[] {
				new TypeMessageFilter(PingRequest.class)
		});
	}

}
