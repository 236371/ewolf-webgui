package il.technion.ewolf.dht.storage;

import il.technion.ewolf.dht.DHTStorage;
import il.technion.ewolf.dht.op.PutOperation;
import il.technion.ewolf.kbr.Key;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import ch.lambdaj.Lambda;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class AgeLimitedDHTStorage implements DHTStorage {

	private final Map<Key, Set<Entry>> storage;
	private final Provider<PutOperation> putOperationProvider;
	private final Timer timer;
	private final long checkInterval;
	
	private class Entry {
		private final Serializable data;
		private final Key key;
		private final long age;
		private final long insertTime;
		
		private Entry(Serializable data, long age, Key key) {
			this.data = data;
			this.key = key;
			this.age = age;
			this.insertTime = System.currentTimeMillis();
		}
		
		public boolean isValid() {
			return (validTime + insertTime) < System.currentTimeMillis();
		}
		
		public boolean isTooOld() {
			return (maxAge + age) < System.currentTimeMillis();
		}
		
		public Serializable getData() {
			return data;
		}
		
		public Key getKey() {
			return key;
		}
		public long getAge() {
			return age;
		}
		
		@Override
		public int hashCode() {
			return data.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null || !getClass().equals(obj.getClass()))
				return false;
			return data.equals(((Entry)obj).getData());
		}
		
		@Override
		public String toString() {
			return data.toString();
		}
	}
	
	private String dhtName = null;
	private long validTime = TimeUnit.HOURS.toMillis(1);
	private boolean rereplicate = false;
	private long maxAge = TimeUnit.DAYS.toMillis(1);
	
	
	
	@Inject
	AgeLimitedDHTStorage(
			@Named("dht.storage.checkInterval") long checkInterval,
			Timer timer,
			Provider<PutOperation> putOperationProvider) {
		
		this.storage = new ConcurrentHashMap<Key, Set<Entry>>();
		this.putOperationProvider = putOperationProvider;
		this.timer = timer;
		this.checkInterval = checkInterval;
		
	}
	
	public AgeLimitedDHTStorage setRereplicate(boolean rereplicate) {
		this.rereplicate = rereplicate;
		return this;
	}
	
	@Override
	public void setDHTName(String dhtName) {
		this.dhtName = dhtName;
	}
	
	public AgeLimitedDHTStorage setMaxAge(long maxAge) {
		this.maxAge = maxAge;
		return this;
	}
	
	public AgeLimitedDHTStorage setValidTime(long validTime) {
		this.validTime = validTime;
		return this;
	}
	
	public AgeLimitedDHTStorage create() {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				removeAllOldEntries();
			}
		}, checkInterval, checkInterval);
		return this;
	}
	
	private void removeOldEntries(Set<Entry> entries, Collection<Entry> toReinsert) {
		if (dhtName == null)
			throw new IllegalStateException("missing dht name for this storage");
		
		synchronized (entries) {
			Iterator<Entry> i = entries.iterator();
			while (i.hasNext()) {
				Entry e = i.next();
				if (!e.isValid()) {
					i.remove();
					if (rereplicate && !e.isTooOld())
						toReinsert.add(e);
				}
			}
		}
	}
	
	private void removeAllOldEntries() {
		List<Entry> toReinsert = new ArrayList<Entry>();
		
		//System.out.println("Removing old entries");
		
		synchronized (storage) {
			for (Set<Entry> entries : storage.values()) {
				removeOldEntries(entries, toReinsert);
			}
		}
		if (toReinsert.isEmpty())
			return;
		
		System.out.println("rereplicating: "+toReinsert);
		for (Entry e : toReinsert) {
			try {
				putOperationProvider.get()
					.setKey(e.getKey())
					.setData(e.getData())
					.setDhtName(dhtName)
					.setStorage(this)
					.setAge(e.getAge())
					.doPut();
			} catch (Exception e1) {}
		}
	}
	
	@Override
	public void store(Key key, long age, Serializable data) {
		Set<Entry> s = null;
		synchronized (storage) {
			 s = storage.get(key);
			 if (s == null) {
				 s = new HashSet<Entry>();
				 storage.put(key, s);
			 }
		}
		
		Entry e = new Entry(data, age, key);
		synchronized (s) {
			if (!s.add(e)) {
				s.remove(e);
				s.add(e);
			}
		}
	}

	@Override
	public Set<Serializable> search(Key key) {
		Set<Entry> res = storage.get(key);
		if (res == null)
			return Collections.emptySet();

		synchronized (res) {
			return new HashSet<Serializable>(Lambda.extract(res, Lambda.on(Entry.class).getData()));
		}
	}

}
