package il.technion.ewolf.socialfs;

import il.technion.ewolf.kbr.Key;

import java.io.Serializable;


public class UserID implements Serializable {

	private static final long serialVersionUID = 7189639756659919284L;

	private final Key key;


	public UserID(Key key) {
		this.key = key;
	}
	
	public Key getKey() {
		return key;
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(getClass().equals(obj.getClass())))
			return false;
		return key.equals(((UserID)obj).getKey());
	}
	
	@Override
	public String toString() {
		return key.toBase64();
	}
}
