package il.technion.ewolf.socialfs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

public class Credentials implements Serializable {

	private static final long serialVersionUID = 4950812426114169711L;

	// NEVER REMOVE THE TRANSIENT MODIFIER !!!
	private transient SecretKey credentialsKey = null;
	
	private final PrivateKey prvSigKey;
	private final PrivateKey prvEncKey;
	private final SecretKey groupsMasterKey;
	
	private final Profile profile;
	
	
	Credentials(
			SecretKey credentialsKey,
			PrivateKey prvSigKey,
			PrivateKey prvEncKey,
			SecretKey groupsMasterKey,
			Profile profile) {
		
		this.credentialsKey = credentialsKey;
		this.prvSigKey = prvSigKey;
		this.prvEncKey = prvEncKey;
		this.groupsMasterKey = groupsMasterKey;
		this.profile = profile;
		
		if (!profile.getPrvSigKey().equals(prvSigKey))
			throw new IllegalArgumentException("sig keys dont match");
	}
	
	public Credentials setCredentialsKey(SecretKey credentialsKey) {
		this.credentialsKey = credentialsKey;
		return this;
	}
	
	public PrivateKey getPrvEncKey() {
		return prvEncKey;
	}
	
	public PrivateKey getPrvSigKey() {
		return prvSigKey;
	}
	
	public SecretKey getCredentialsKey() {
		return credentialsKey;
	}


	public Profile getProfile() {
		return profile;
	}
	
	public SecretKey getGroupsMasterKey() {
		return groupsMasterKey;
	}
	
	
	private void writeObject(ObjectOutputStream out) throws IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
		out.defaultWriteObject();
	}
	
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		profile.setPrvSigKey(prvSigKey);
	}
	
	
	@Override
	public String toString() {
		return 
			"credentialsKey: "+Base64.encodeBase64String(credentialsKey.getEncoded())+"\n\n"+
			"privSigKey: "+prvSigKey+"\n\n"+
			"privEncKey: "+prvEncKey+"\n\n"+
			"groupsMasterKey: "+Base64.encodeBase64String(groupsMasterKey.getEncoded())+"\n\n"+
			"profile: \n"+profile.toString();
	}
}
