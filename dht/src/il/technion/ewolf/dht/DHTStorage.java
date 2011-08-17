package il.technion.ewolf.dht;

import javax.inject.Named;

import com.google.inject.Inject;

public class DHTStorage {

	
	@Inject
	DHTStorage(
			@Named("dht.storage.maxlifetime") long maxLifetime,
			@Named("dht.storage.maxentrysize") int maxEntrySize,
			@Named("dht.storage.maxentries") int maxEntries) {
		
	}
	
}
