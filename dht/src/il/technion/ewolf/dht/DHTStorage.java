package il.technion.ewolf.dht;

import static ch.lambdaj.Lambda.*;
import il.technion.ewolf.kbr.Key;

import java.util.Collection;

import javax.inject.Named;

import com.google.inject.Inject;

class DHTStorage {

	
	@Inject
	DHTStorage(
			@Named("dht.storage.maxlifetime") long maxLifetime,
			@Named("dht.storage.maxentrysize") int maxEntrySize,
			@Named("dht.storage.maxentries") int maxEntries) {
		
		
	}
	
	public void store(Key key, Collection<String> vals) {
	}
	
	public Collection<String> get(Key key) {
		
		return null;
	}
	
	
	
}
