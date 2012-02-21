package il.technion.ewolf.stash.crypto;

import il.technion.ewolf.stash.exception.EncryptionException;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class AsymmetricEncryptedObject<T extends Serializable> implements Serializable {

	private transient SecretKey secretKey;
	
	private static final long serialVersionUID = 7684904233698104363L;
	
	private EncryptedObject<T> encryptedData;
	private EncryptedObject<SecretKey> symmetricKey;
	
	@Inject
	AsymmetricEncryptedObject(@Named("stash.random.secretkey") SecretKey secretKey) {
		this.secretKey = secretKey;
	}
	
	public AsymmetricEncryptedObject<T> encrypt(T obj, PublicKey pubKey) throws EncryptionException {
		symmetricKey = new EncryptedObject<SecretKey>().encrypt(secretKey, pubKey);
		encryptedData = new EncryptedObject<T>().encrypt(obj, secretKey);
		return this;
	}
	
	public T decrypt(PrivateKey privKey) throws IOException, InvalidKeyException, ClassNotFoundException {
		secretKey = symmetricKey.decrypt(privKey);
		return encryptedData.decrypt(secretKey);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		
		@SuppressWarnings("unchecked")
		AsymmetricEncryptedObject<T> o = (AsymmetricEncryptedObject<T>)obj;
		
		return o.symmetricKey.equals(o.symmetricKey) && encryptedData.equals(o.encryptedData);
	}
	
	@Override
	public int hashCode() {
		return symmetricKey.hashCode();
	}
	
}
