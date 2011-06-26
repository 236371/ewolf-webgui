package dht.openkad;

import java.io.IOException;

public interface KadConnectionListener {

	public void onIncomingConnection(KadConnection conn) throws IOException;
	
}
