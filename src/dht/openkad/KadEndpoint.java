package dht.openkad;

import java.io.IOException;

public abstract class KadEndpoint {

	public abstract void publish(KadConnectionListener listener) throws IOException;
	
	public abstract void shutdown();
}
