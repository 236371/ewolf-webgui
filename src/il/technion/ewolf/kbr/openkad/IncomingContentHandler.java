package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.MessageHandler;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.msg.ContentMessage;
import il.technion.ewolf.kbr.openkad.msg.ContentRequest;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.net.KadServer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class IncomingContentHandler<A> implements CompletionHandler<KadMessage, A> {

	// state
	private String tag;
	private MessageHandler handler;
	
	// dependencies
	private final ExecutorService clientExecutor;
	private final Node localNode;
	private final KadServer kadServer;
	
	@Inject
	IncomingContentHandler(
			@Named("openkad.executors.client") ExecutorService clientExecutor,
			@Named("openkad.local.node") Node localNode,
			KadServer kadServer) {
		this.kadServer = kadServer;
		this.clientExecutor = clientExecutor;
		this.localNode = localNode;
	}
	
	public IncomingContentHandler<A> setHandler(MessageHandler handler) {
		this.handler = handler;
		return this;
	}
	
	public IncomingContentHandler<A> setTag(String tag) {
		this.tag = tag;
		return this;
	}
	
	
	private void handleRequest(final ContentRequest req) {
		// execute the user's handler and send back the result
		clientExecutor.execute(new Runnable() {
			@Override
			public void run() {
				byte[] resContent = handler.onIncomingRequest(req.getSrc(), tag, req.getContent());
				try {
					kadServer.send(req.getSrc(), req
						.generateResponse(localNode)
						.setContent(resContent));
				} catch (IOException e) {
					// unable to send response back, nothing to do
					e.printStackTrace();
				}
			}
		});
	}
	
	private void handleMessage(final ContentMessage msg) {
		// execute the user's handler and send back the result
		clientExecutor.execute(new Runnable() {
			@Override
			public void run() {
				handler.onIncomingMessage(msg.getSrc(), tag, msg.getContent());
			}
		});
	}
	
	
	@Override
	public void completed(final KadMessage msg, A attachment) {
		if (msg instanceof ContentRequest) {
			handleRequest((ContentRequest)msg);
		} else if (msg instanceof ContentMessage) {
			handleMessage((ContentMessage)msg);
		}
	}

	@Override
	public void failed(Throwable exc, A attachment) {
		// should never be here
	}

}
