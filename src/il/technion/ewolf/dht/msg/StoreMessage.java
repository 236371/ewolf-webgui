package il.technion.ewolf.dht.msg;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;

import com.google.inject.Inject;

public class StoreMessage extends DHTMessage {
	
	private static final long serialVersionUID = 4824611006605171348L;
	
	private Key key;
	private Serializable data;
	private long age;
	
	@Inject
	StoreMessage() {
		
	}
	
	public StoreMessage setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public StoreMessage setContent(Serializable data) {
		this.data = data;
		return this;
	}
	
	public StoreMessage setAge(long age) {
		this.age = age;
		return this;
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
}
