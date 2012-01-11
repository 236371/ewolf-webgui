package il.technion.ewolf.kbr;

public interface MessageHandler {
	
	void onIncomingMessage(Node from, String tag, byte[] content);
	byte[] onIncomingRequest(Node from, String tag, byte[] content);
}
