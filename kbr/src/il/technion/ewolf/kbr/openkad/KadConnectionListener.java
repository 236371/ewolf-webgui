package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.io.IOException;


public interface KadConnectionListener {

	public void onIncomingConnection(KadConnection conn) throws IOException;
	public void onIncomingMessage(KadMessage msg, KadMessageBuilder response) throws IOException;
	
}
