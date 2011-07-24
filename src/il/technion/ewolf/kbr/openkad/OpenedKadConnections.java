package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


import com.google.inject.Inject;

public class OpenedKadConnections {

	private final Map<Key, KadConnection> connFromKey = 
			Collections.synchronizedMap(new HashMap<Key, KadConnection>());
	
	private final KBuckets kBuckets;
	
	@Inject
	OpenedKadConnections(KBuckets kBuckets) {
		this.kBuckets = kBuckets;
	}
	
	
	public boolean keepAlive(KadConnection conn, KadMessage msg) {
		if (!msg.isKeepAlive())
			return false;
		
		if (!kBuckets.addProxyEndpoint(msg.getLastHop()))
			return false;
		
		connFromKey.put(msg.getLastHop().getKey(), conn);
		return true;
	}
	
	public KadConnection get(Key k) {
		return connFromKey.get(k);
	}
	
	public Collection<Key> getAllContacts() {
		synchronized (connFromKey) {
			return new HashSet<Key>(connFromKey.keySet());
		}
	}
	
	public void remove(Key k) {
		if (k == null)
			return;
		connFromKey.remove(k);
		// TODO: remove from kBuckets also
	}
	
}
