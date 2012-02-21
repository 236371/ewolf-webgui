package il.technion.ewolf.stash;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.stash.crypto.EncryptedObject;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Group implements Serializable {

	private static final long serialVersionUID = -8278765626891367179L;
	
	private final Key groupId;
	private final SecretKey groupSecretKey;
	
	@Inject
	Group(
			@Named("stash.random.key") Key groupId,
			@Named("stash.random.secretkey") SecretKey groupSecretKey) {
		
		this.groupId = groupId;
		this.groupSecretKey = groupSecretKey;
	}
	
	
	EncryptedChunk encrypt(Serializable obj) {
		try {
			return new EncryptedChunk(groupId, new EncryptedObject<Serializable>().encrypt(obj, groupSecretKey));
		} catch (Exception e) {
			throw new RuntimeException("ecryption exception", e);
		}
	}
	
	
	Serializable decrypt(EncryptedObject<Serializable> encObj) throws InvalidKeyException, IOException, ClassNotFoundException {
		return encObj.decrypt(groupSecretKey);
	}


	public Key getGroupId() {
		return groupId;
	}
	
	public SecretKey getGroupSecretKey() {
		return groupSecretKey;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		
		Group g = (Group)obj;
		return getGroupId().equals(g.getGroupId()) && getGroupSecretKey().equals(g.getGroupSecretKey());
	}
	
	@Override
	public int hashCode() {
		return groupId.hashCode() + groupSecretKey.hashCode();
	}
	
	@Override
	public String toString() {
		return "Group key: "+groupId+", secret key: "+Base64.encodeBase64String(groupSecretKey.getEncoded());
	}
}
