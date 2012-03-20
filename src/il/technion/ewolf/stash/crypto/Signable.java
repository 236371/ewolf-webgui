package il.technion.ewolf.stash.crypto;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public abstract class Signable implements Serializable {

	private static final long serialVersionUID = -949472370371441074L;

	// NEVER REMOVE TRANSIENT KEYWORD !!!
	private transient PrivateKey prvSigKey = null;
	
	private PublicKey pubSigKey = null;
	private byte[] signature = null;
	
	
	protected Signable() {
		
	}

	protected Signable(
			PrivateKey privSigKey,
			PublicKey pubSigKey) {
		this.prvSigKey = privSigKey;
		this.pubSigKey = pubSigKey;
	}
	
	
	public PrivateKey getPrvSigKey() {
		return prvSigKey;
	}
	
	public Signable setPubSigKey(PublicKey pubSigKey) {
		this.pubSigKey = pubSigKey;
		return this;
	}
	
	public PublicKey getPubSigKey() {
		return pubSigKey;
	}
	
	public Signable setPrvSigKey(PrivateKey privSigKey) {
		this.prvSigKey = privSigKey;
		return this;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
		
		if (signature != null) {
			// simply serialize this object, since the sig already exists
			// I dont need to generate it
			out.defaultWriteObject();
			return;
		}
		
		// no signature was found, generating one !
		
		// missing pubSigKey  ==> other side will not be able to verify
		// missing privSigKey ==> cannot sign
		if (pubSigKey == null || prvSigKey == null)
			throw new NotSerializableException("missing sig keys");
		
		Signature s = Signature.getInstance(prvSigKey.getAlgorithm());
		s.initSign(prvSigKey);
		
		s.update(pubSigKey.getEncoded());
		updateSignature(s);
		
		signature = s.sign();
		
		out.defaultWriteObject();
	}
	
	protected abstract void updateSignature(Signature sig) throws SignatureException;
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.registerValidation(new ObjectInputValidation() {
			
			@Override
			public void validateObject() throws InvalidObjectException {
				try {
					Signable.this.verifySignature();
				} catch (Exception e) {
					throw new InvalidObjectException(e.getMessage());
				}
			}
		}, 0);
		
		in.defaultReadObject();
	}
	
	protected void resetSignature() {
		signature = null;
	}
	
	protected void verify() throws Exception {
	}
	private final void verifySignature() throws Exception {
		Signature s = Signature.getInstance(pubSigKey.getAlgorithm());
		s.initVerify(pubSigKey);
		
		s.update(pubSigKey.getEncoded());
		updateSignature(s);
		
		if (!s.verify(signature))
			throw new SignatureException("wrong signature");
		
		verify();
	}
	
	
	byte[] getSignature() {
		return signature;
	}
	
}
