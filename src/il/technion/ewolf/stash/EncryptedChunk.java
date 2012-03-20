package il.technion.ewolf.stash;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.stash.crypto.EncryptedObject;

import java.io.Serializable;

public class EncryptedChunk implements Serializable {

	private static final long serialVersionUID = 8401815489219617355L;
	
	private final Key groupId;
	private final EncryptedObject<Serializable> data;
	
	
	EncryptedChunk(
			Key groupId,
			EncryptedObject<Serializable> data) {
		this.groupId = groupId;
		this.data = data;
	}
	
	public Key getGroupId() {
		return groupId;
	}
	
	public EncryptedObject<Serializable> getData() {
		return data;
	}

	@Override
	public int hashCode() {
		return groupId.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		EncryptedChunk o = (EncryptedChunk)obj;
		return groupId.equals(o.getGroupId()) && data.equals(o.getData());
	}
	
}
