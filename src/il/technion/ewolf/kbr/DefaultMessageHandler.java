package il.technion.ewolf.kbr;

public class DefaultMessageHandler implements MessageHandler {

	@Override
	public void onIncomingMessage(Node from, String tag, byte[] content) {
	}

	@Override
	public byte[] onIncomingRequest(Node from, String tag, byte[] content) {
		return null;
	}

}
