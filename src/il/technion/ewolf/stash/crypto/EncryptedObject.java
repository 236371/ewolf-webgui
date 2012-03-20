package il.technion.ewolf.stash.crypto;

import il.technion.ewolf.stash.exception.EncryptionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;

public class EncryptedObject<T extends Serializable> implements Serializable {

	private static final long serialVersionUID = 8623936108199131904L;
	
	private byte[] bytes;
	
	
	public EncryptedObject() {
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public EncryptedObject<T> encrypt(T obj, Key key) throws EncryptionException {
		ByteArrayOutputStream bout = null;
		//CipherOutputStream cout = null;
		ObjectOutputStream oout = null;
		
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(key.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, key);
		} catch (Exception e) {
			e.printStackTrace();
			throw new EncryptionException(e);
		}
		try {
			bout = new ByteArrayOutputStream();
			//cout = new CipherOutputStream(bout, cipher);
			//oout = new ObjectOutputStream(cout);
			oout = new ObjectOutputStream(bout);
			
			oout.writeObject(obj);
						
		} catch (IOException e) {
			throw new AssertionError(e);
		} finally {
			try { oout.close(); } catch (Exception e) {}
			//try { cout.close(); } catch (Exception e) {}
			try { bout.close(); } catch (Exception e) {}
		}
		
		try {
			bytes = cipher.doFinal(bout.toByteArray());
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			throw new AssertionError("wrong encryption algorithm: "+e.getMessage());
		} catch (BadPaddingException e) {
			e.printStackTrace();
			throw new AssertionError("wrong encryption algorithm: "+e.getMessage());
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public T decrypt(Key key) throws IOException, InvalidKeyException, ClassNotFoundException {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(key.getAlgorithm());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		cipher.init(Cipher.DECRYPT_MODE, key);
		
		InputStream bin = null;
		CipherInputStream cin = null;
		ObjectInputStream oin = null;
		try {
			//System.out.println("decrypt: "+Base64.encodeBase64String(bytes));
			bin = new ByteArrayInputStream(bytes);
			cin = new CipherInputStream(bin, cipher);
			oin = new ObjectInputStream(cin);
			
			return (T)oin.readObject();
			
		} finally {
			try { oin.close(); } catch (Exception e) {}
			try { cin.close(); } catch (Exception e) {}
			try { bin.close(); } catch (Exception e) {}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		
		@SuppressWarnings("unchecked")
		EncryptedObject<T> o = (EncryptedObject<T>)obj;
		
		return Arrays.equals(bytes, o.bytes);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}
	
}
